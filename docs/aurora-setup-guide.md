# Aurora PostgreSQL 구축 가이드
> Once Global News — 프로덕션 DB 셋업 (AWS 아키텍처 기준)
> 작성일: 2026-04-18

---

## 목차
1. [아키텍처 개요](#1-아키텍처-개요)
2. [사전 준비 (VPC / 보안그룹)](#2-사전-준비-vpc--보안그룹)
3. [Aurora 클러스터 생성](#3-aurora-클러스터-생성)
4. [DB 초기화 — 스키마 적용](#4-db-초기화--스키마-적용)
5. [Spring Boot 환경변수 연결](#5-spring-boot-환경변수-연결)
6. [HikariCP 커넥션 풀 튜닝](#6-hikaricp-커넥션-풀-튜닝)
7. [운영 체크리스트](#7-운영-체크리스트)
8. [장애 대응](#8-장애-대응)

---

## 1. 아키텍처 개요

```
Internet
    │
    ▼
[ALB / EC2 — Spring Boot]
    │  (Private Subnet, 5432)
    ▼
[Aurora PostgreSQL Cluster]
  ├── Writer Instance  (ap-northeast-2a)
  └── Reader Instance  (ap-northeast-2c)  ← 읽기 부하 분산용
```

**선택 근거**
| 항목 | 선택 | 이유 |
|---|---|---|
| 엔진 | Aurora PostgreSQL 16 | PostgreSQLDialect 그대로 호환, 최대 5배 성능 |
| 인스턴스 | `db.t3.medium` (초기) | 비용 절감, 트래픽 증가 시 `db.r6g.large`로 변경 |
| 가용성 | Multi-AZ (Writer 1 + Reader 1) | 자동 Failover 60초 이내 |
| 스토리지 | Aurora 자동 확장 (10 GB → 최대 128 TiB) | 별도 디스크 관리 불필요 |

---

## 2. 사전 준비 (VPC / 보안그룹)

### 2-1. DB 전용 보안그룹 생성

```bash
# AWS CLI 기준
aws ec2 create-security-group \
  --group-name once-aurora-sg \
  --description "Aurora PostgreSQL SG for Once Backend" \
  --vpc-id <YOUR_VPC_ID>

# EC2 (백엔드 서버) → Aurora 5432 포트 허용
aws ec2 authorize-security-group-ingress \
  --group-id <AURORA_SG_ID> \
  --protocol tcp \
  --port 5432 \
  --source-group <EC2_SG_ID>
```

> **주의:** 퍼블릭(0.0.0.0/0) 오픈 절대 금지. EC2 보안그룹 ID로만 인바운드 허용.

### 2-2. DB 서브넷 그룹 생성 (콘솔 또는 CLI)

- RDS → 서브넷 그룹 → 생성
- VPC 선택 후 **Private Subnet** 2개 이상 추가 (가용 영역 분리)
- 이름: `once-aurora-subnet-group`

---

## 3. Aurora 클러스터 생성

### 3-1. AWS 콘솔 기준 단계별 설정

| 설정 항목 | 값 |
|---|---|
| 엔진 | Amazon Aurora — PostgreSQL 호환 |
| 버전 | Aurora PostgreSQL 16.x (최신 안정) |
| 템플릿 | 프로덕션 |
| 클러스터 ID | `once-aurora-cluster` |
| DB 인스턴스 ID | `once-aurora-instance-1` |
| 마스터 사용자 | `oncedba` |
| 마스터 비밀번호 | AWS Secrets Manager 자동 생성 사용 (권장) |
| 인스턴스 클래스 | `db.t3.medium` |
| Multi-AZ | 예 (Reader 인스턴스 추가) |
| VPC | 운영 VPC 선택 |
| 서브넷 그룹 | `once-aurora-subnet-group` |
| 보안그룹 | `once-aurora-sg` |
| 퍼블릭 액세스 | **아니요** |
| 포트 | 5432 |
| 초기 DB 이름 | `once_db` |
| 백업 보존 기간 | 7일 |
| 암호화 | 활성화 (KMS 기본 키) |
| 마이너 버전 자동 업그레이드 | 비활성화 (직접 제어 권장) |

### 3-2. 파라미터 그룹 설정 (선택 권장)

```bash
# 클러스터 파라미터 그룹 생성
aws rds create-db-cluster-parameter-group \
  --db-cluster-parameter-group-name once-aurora-params \
  --db-parameter-group-family aurora-postgresql16 \
  --description "Once Aurora Cluster Parameters"

# 타임존 설정 (Asia/Seoul)
aws rds modify-db-cluster-parameter-group \
  --db-cluster-parameter-group-name once-aurora-params \
  --parameters "ParameterName=timezone,ParameterValue=Asia/Seoul,ApplyMethod=immediate"
```

### 3-3. 엔드포인트 확인 (생성 후)

```
Writer: once-aurora-cluster.cluster-xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com
Reader: once-aurora-cluster.cluster-ro-xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com
```

---

## 4. DB 초기화 — 스키마 적용

### 4-1. EC2에서 psql 접속

```bash
# EC2 접속 후 psql 설치 (Amazon Linux 2)
sudo yum install -y postgresql15

# Aurora Writer 엔드포인트로 접속
psql -h once-aurora-cluster.cluster-xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com \
     -U oncedba \
     -d once_db \
     -W
```

### 4-2. 스키마 적용 전 DROP 문 제거

> **⚠️ 중요:** `schema-postgresql.sql` 상단의 DROP 문은 **운영 환경에서 절대 실행 금지**
> 아래 명령으로 DROP 없이 안전하게 적용

```bash
# DROP 라인 제외하고 적용
grep -v "^DROP" schema-postgresql.sql | \
psql -h <WRITER_ENDPOINT> -U oncedba -d once_db -W
```

또는 파일을 직접 수정 후 적용:

```bash
# DROP 구문 5줄 삭제 후 적용 (최초 배포 시)
psql -h <WRITER_ENDPOINT> -U oncedba -d once_db -W -f schema-postgresql.sql
```

> 최초 배포 시에는 DROP 포함 전체 실행 가능. **이후 재실행 시 반드시 DROP 제거.**

### 4-3. 스키마 적용 확인

```sql
-- 접속 후 테이블 목록 확인
\dt

-- 예상 출력:
--  Schema |      Name        | Type  |  Owner
-- --------+------------------+-------+---------
--  public | chat_attachments | table | oncedba
--  public | chat_messages    | table | oncedba
--  public | chat_rooms       | table | oncedba
--  public | reviews          | table | oncedba
--  public | users            | table | oncedba

-- 인덱스 확인
\di

-- 트리거 확인
SELECT trigger_name, event_object_table FROM information_schema.triggers;
```

### 4-4. 애플리케이션 전용 DB 유저 생성 (보안 강화)

```sql
-- 마스터 계정으로 접속 후 실행
CREATE USER onceapp WITH PASSWORD '<강력한_랜덤_비밀번호>';

-- 필요한 권한만 부여 (최소 권한 원칙)
GRANT CONNECT ON DATABASE once_db TO onceapp;
GRANT USAGE ON SCHEMA public TO onceapp;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO onceapp;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO onceapp;

-- 이후 생성 테이블에도 자동 권한 부여
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO onceapp;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO onceapp;
```

> **운영 시 `oncedba` (마스터)는 인프라 작업 전용, 앱은 `onceapp` 계정만 사용**

---

## 5. Spring Boot 환경변수 연결

### 5-1. `application-prod.yaml` 에 주입될 환경변수 목록

| 환경변수 | 값 예시 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://<WRITER_ENDPOINT>:5432/once_db` | Aurora Writer 엔드포인트 |
| `DB_USERNAME` | `onceapp` | 앱 전용 DB 계정 |
| `DB_PASSWORD` | `<Secrets Manager에서 가져옴>` | DB 비밀번호 |

### 5-2. EC2에 환경변수 설정 방법 (권장: AWS Secrets Manager)

**방법 A — AWS Secrets Manager + EC2 IAM Role (권장)**

```bash
# Secrets Manager에 DB 자격증명 저장
aws secretsmanager create-secret \
  --name "once/prod/db" \
  --description "Once Backend DB credentials" \
  --secret-string '{
    "DB_URL": "jdbc:postgresql://<WRITER_ENDPOINT>:5432/once_db",
    "DB_USERNAME": "onceapp",
    "DB_PASSWORD": "<password>"
  }'

# EC2 시작 스크립트 (User Data 또는 systemd 서비스)에서 주입
export DB_URL=$(aws secretsmanager get-secret-value \
  --secret-id once/prod/db \
  --query 'SecretString' | jq -r '.DB_URL')
export DB_USERNAME=$(aws secretsmanager get-secret-value \
  --secret-id once/prod/db \
  --query 'SecretString' | jq -r '.DB_USERNAME')
export DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id once/prod/db \
  --query 'SecretString' | jq -r '.DB_PASSWORD')
```

**방법 B — systemd 환경변수 파일 (간단한 방법)**

```ini
# /etc/once-backend/env.conf
DB_URL=jdbc:postgresql://<WRITER_ENDPOINT>:5432/once_db
DB_USERNAME=onceapp
DB_PASSWORD=<password>
SPRING_PROFILES_ACTIVE=prod
```

```ini
# /etc/systemd/system/once-backend.service
[Unit]
Description=Once Global News Backend
After=network.target

[Service]
User=ec2-user
EnvironmentFile=/etc/once-backend/env.conf
ExecStart=/usr/bin/java -jar /opt/once-backend/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable once-backend
sudo systemctl start once-backend
```

### 5-3. 프로파일 활성화 확인

```bash
# application-prod.yaml 이 로드되어야 함
java -jar app.jar --spring.profiles.active=prod
```

---

## 6. HikariCP 커넥션 풀 튜닝

현재 `application-prod.yaml` 설정값과 Aurora 인스턴스 기준 권장값:

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30        # db.t3.medium max_connections=170, 30은 적정
      connection-timeout: 30000    # 30초
      idle-timeout: 600000         # 10분
      max-lifetime: 1800000        # 30분 (Aurora 재연결 주기보다 짧게)
      connection-test-query: SELECT 1
      pool-name: OnceProdHikariCP
```

> **`max-lifetime`을 1800000(30분)으로 유지하는 이유:**
> Aurora는 장시간 유휴 연결을 끊을 수 있으며, RDS Proxy 없이 직접 연결 시 `max-lifetime < DB wait_timeout` 이어야 stale connection 오류 방지.

---

## 7. 운영 체크리스트

### 배포 전
- [ ] Aurora 클러스터 상태: `Available`
- [ ] 보안그룹 인바운드: EC2 SG → 5432만 허용
- [ ] 퍼블릭 액세스: `No`
- [ ] 스키마 적용 완료 (`\dt` 로 5개 테이블 확인)
- [ ] `onceapp` 유저 권한 부여 완료
- [ ] 환경변수 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) 주입 확인
- [ ] `SPRING_PROFILES_ACTIVE=prod` 설정
- [ ] `ddl-auto: validate` — JPA가 스키마 불일치 시 기동 실패하므로 스키마 일치 필수

### 배포 후
- [ ] 헬스체크: `GET /actuator/health` → `{"status":"UP"}`
- [ ] CloudWatch → RDS 메트릭: `DatabaseConnections`, `FreeableMemory` 확인
- [ ] 슬로우 쿼리 로그 활성화 (파라미터: `log_min_duration_statement=1000`)

### 백업 확인
- [ ] 자동 백업 보존 기간: 7일 설정 확인
- [ ] 스냅샷 수동 생성 (초도 배포 직후)

```bash
aws rds create-db-cluster-snapshot \
  --db-cluster-identifier once-aurora-cluster \
  --db-cluster-snapshot-identifier once-aurora-initial-$(date +%Y%m%d)
```

---

## 8. 장애 대응

### Failover 발생 시 (Writer → Reader 자동 승격)
- Aurora는 약 30~60초 내 자동 Failover
- Spring Boot는 `DB_URL`에 **클러스터 엔드포인트(Writer)** 를 사용하므로 DNS TTL 내 자동 재연결됨
- HikariCP `connection-timeout: 30000` 설정으로 재시도 커버

```bash
# Failover 강제 테스트
aws rds failover-db-cluster --db-cluster-identifier once-aurora-cluster
```

### 연결 거부 (`Connection refused`) 발생 시 체크리스트

```
1. EC2 → Aurora 보안그룹 5432 인바운드 확인
2. Aurora 인스턴스 상태 확인 (AWS 콘솔 → RDS)
3. DB_URL 엔드포인트가 Writer 엔드포인트인지 확인
4. psql 직접 접속 테스트:
   psql -h <WRITER_ENDPOINT> -U onceapp -d once_db -W
5. HikariCP 풀 고갈 여부: /actuator/metrics/hikaricp.connections.active
```

### 스키마 변경 시 (운영 중)

```bash
# 1. 스냅샷 먼저 생성
aws rds create-db-cluster-snapshot ...

# 2. DDL 실행 (ALTER TABLE 등 — DROP 절대 금지)
psql -h <WRITER_ENDPOINT> -U oncedba -d once_db -W

# 3. application-prod.yaml ddl-auto: validate 이므로
#    스키마 변경 후 반드시 앱 재기동
```

---

## 참고: 현재 프로젝트 테이블 구조 요약

| 테이블 | 설명 | 주요 인덱스 |
|---|---|---|
| `users` | 카카오 OAuth 사용자 | `kakao_id`, `email`, `nickname` |
| `chat_rooms` | AI 채팅 세션 | `user_id`, `article_id`, `created_at` |
| `chat_messages` | 채팅 메시지 (USER/ASSISTANT/SYSTEM) | `room_id`, `created_at` |
| `chat_attachments` | S3 첨부파일 메타데이터 | `user_id`, `room_id`, `message_id`, `status` |
| `reviews` | AI 서비스 후기 (별점 1~5) | `user_id`, `created_at` |

> 모든 테이블에 `updated_at` 자동 갱신 트리거 적용됨