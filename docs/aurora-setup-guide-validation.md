# aurora-setup-guide.md 검증 보고서
> 검증 기준: 프로젝트 착수 보고서 + Aurora Global Database 기술 의사결정 문서
> 검증일: 2026-04-18

---

## 검증 요약

| 구분 | 항목 수 | 판정 |
|---|---|---|
| 치명적 오류 (즉시 수정 필요) | 5건 | ❌ CRITICAL |
| 누락 항목 (보완 필요) | 7건 | ⚠️ MISSING |
| 정확한 항목 | 9건 | ✅ PASS |

> **결론: 현재 가이드는 단일 리전 Aurora 클러스터 구축 가이드이며, 프로젝트가 요구하는 Aurora Global Database 기반 멀티리전 DR 구성과 근본적으로 다르다. 구조적 재작성이 필요하다.**

---

## 1. 치명적 오류 (❌ CRITICAL)

### 1-1. Aurora 단일 클러스터 vs Aurora Global Database

**현재 가이드:**
```
[Aurora PostgreSQL Cluster]
  ├── Writer Instance  (ap-northeast-2a)
  └── Reader Instance  (ap-northeast-2c)
```

**프로젝트 요구:**
```
[Aurora Global Database]
  ├── Primary Cluster  — Seoul (ap-northeast-2)
  │     ├── Writer Instance (ap-northeast-2a)
  │     └── Reader Instance (ap-northeast-2c)
  └── Secondary Cluster — Tokyo (ap-northeast-1)  ← DR 리전
        └── Reader Instance (ap-northeast-1a)
```

**문제점:** 현재 가이드는 서울 단일 클러스터만 구성한다. Aurora Global Database는 별도 생성 단계가 필요하며, 단순 클러스터 생성과 완전히 다른 API와 콘솔 플로우를 사용한다. 이 상태로 구축하면 도쿄 리전 DR 자체가 존재하지 않는다.

**수정 방향:**
```bash
# Aurora Global Database 생성 (단일 클러스터 생성 후 별도 진행)
aws rds create-global-cluster \
  --global-cluster-identifier once-global-cluster \
  --source-db-cluster-identifier arn:aws:rds:ap-northeast-2:<ACCOUNT>:cluster:once-aurora-cluster \
  --engine aurora-postgresql

# 도쿄 리전에 Secondary 추가
aws rds create-db-cluster \
  --db-cluster-identifier once-aurora-cluster-tokyo \
  --global-cluster-identifier once-global-cluster \
  --engine aurora-postgresql \
  --engine-version 16.x \
  --db-subnet-group-name once-aurora-subnet-group-tokyo \
  --region ap-northeast-1
```

---

### 1-2. 잘못된 Failover API 명령어

**현재 가이드:**
```bash
aws rds failover-db-cluster --db-cluster-identifier once-aurora-cluster
```

**문제점:** `failover-db-cluster`는 **단일 클러스터 내** Writer/Reader 전환 명령이다. 리전 간 Failover에는 완전히 다른 API를 사용해야 한다.

**올바른 명령어:**
```bash
# Cross-Region Failover (비계획적 — 장애 상황)
aws rds failover-global-cluster \
  --global-cluster-identifier once-global-cluster \
  --target-db-cluster-identifier arn:aws:rds:ap-northeast-1:<ACCOUNT>:cluster:once-aurora-cluster-tokyo \
  --region ap-northeast-2

# Cross-Region Switchback (계획적 — 복구 상황, 데이터 유실 없음)
aws rds switchover-global-cluster \
  --global-cluster-identifier once-global-cluster \
  --target-db-cluster-identifier arn:aws:rds:ap-northeast-2:<ACCOUNT>:cluster:once-aurora-cluster \
  --region ap-northeast-1
```

---

### 1-3. 인스턴스 타입 불일치

**현재 가이드:** `db.t3.medium`

**기술 의사결정 문서 확정값:** `db.t4g.medium` × 3 (Seoul Writer 1 + Seoul Reader 1 + Tokyo Reader 1)

**문제점:** `db.t3.medium`은 Aurora Global Database를 지원하지 않는 인스턴스 타입이다. Aurora Global Database는 Graviton 기반(`db.r6g`, `db.t4g`) 또는 `db.r5` 이상을 요구한다. `db.t3.medium`으로 생성 시 Global Database 연결 단계에서 오류가 발생할 수 있다.

---

### 1-4. 아키텍처 다이어그램 — EKS 미반영

**현재 가이드:**
```
[ALB / EC2 — Spring Boot]
    │
    ▼
[Aurora PostgreSQL Cluster]
```

**프로젝트 실제 구조 (착수 보고서 7단계 기준):**
```
[ALB]
  │
[EKS — Spring Boot Pod]
  │
[Aurora Global Database]
```

**문제점:** 백엔드 실행 환경이 EC2가 아니라 EKS이다. 환경변수 주입, 보안그룹 출처(Source SG), DB 접속 방식이 모두 다르다. EKS에서는 systemd 환경파일이 아닌 Kubernetes Secret + External Secrets Operator 또는 AWS Secrets Manager CSI Driver를 사용한다.

---

### 1-5. RPO/RTO 수치 불일치

**현재 가이드:**
> "Aurora는 약 30~60초 내 자동 Failover"

**기술 의사결정 문서 확정 목표:**
| 지표 | 목표치 | 측정 메트릭 |
|---|---|---|
| RPO | < 1초 | `AuroraGlobalDBReplicationLag` |
| Failover RTO | < 5분 | Failover 시작 ~ 서비스 정상 응답 |
| Failback RTO | < 15분 | Switchback 시작 ~ 검증 완료 |

**문제점:** 가이드에 적힌 "30~60초"는 단일 클러스터 내 Multi-AZ Failover 수치다. Cross-Region Failover는 최적화 후에도 2.5~5분이 소요되며, 이것이 이 프로젝트의 핵심 검증 대상이다. RTO 최적화 포인트(장애 감지 30초, DNS TTL 30초, EKS 최소 1 Pod 유지 등)가 전혀 반영되어 있지 않다.

---

## 2. 누락 항목 (⚠️ MISSING)

### 2-1. AuroraGlobalDBReplicationLag 모니터링 완전 누락

**누락 내용:** RPO < 1초를 증명하는 핵심 메트릭이 가이드에 없다.

**추가 필요:**
```bash
# CloudWatch Alarm — 복제 지연 1초 초과 시 알림
aws cloudwatch put-metric-alarm \
  --alarm-name "AuroraGlobalDB-ReplicationLag-High" \
  --metric-name AuroraGlobalDBReplicationLag \
  --namespace AWS/RDS \
  --statistic Maximum \
  --period 60 \
  --threshold 1000 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions <SNS_ARN>
```

---

### 2-2. Route 53 Health Check + 자동 Failover 트리거 누락

**누락 내용:** 의사결정 문서의 자동화 파이프라인이 전혀 없다.

```
Route 53 Health Check (30초 간격, 3회 연속 실패)
    → CloudWatch Alarm
    → EventBridge Rule
    → Step Functions 실행
    → Lambda: aws rds failover-global-cluster
    → Lambda: Route 53 DNS 전환
    → Lambda: EKS Tokyo 스케일업
```

현재 가이드는 수동 명령어만 제시한다.

---

### 2-3. Failback 절차 완전 누락

**누락 내용:** 의사결정 문서에서 정의한 3단계 Failback 프로세스가 없다.

| 단계 | 방식 | 내용 |
|---|---|---|
| 1단계 | 자동 | CloudWatch가 Seoul 리소스 정상 확인 → Slack 알림 |
| 2단계 | 수동 판단 | 운영자가 15분 이상 안정 상태 확인 (플래핑 방지) |
| 3단계 | 수동 트리거 → 자동 실행 | `switchover-global-cluster` → DNS 전환 → Tokyo 스케일다운 → 정합성 검증 |

---

### 2-4. 데이터 정합성 검증 절차 누락

**누락 내용:** Failback 후 데이터가 한 건도 유실되지 않았음을 수치로 증명하는 절차.

```sql
-- Failover 직전 기록
SELECT 
  (SELECT COUNT(*) FROM users) AS users_count,
  (SELECT COUNT(*) FROM chat_messages) AS messages_count,
  (SELECT COUNT(*) FROM reviews) AS reviews_count,
  NOW() AS snapshot_time;

-- Failback 후 검증: Tokyo 운영 중 신규 데이터 + Failover 전 데이터 전부 존재 확인
-- checksum 비교, row count 비교를 CloudWatch Custom Metric으로 자동 기록
```

---

### 2-5. Spring Boot DB 재연결 Retry 로직 누락

**의사결정 문서 요구사항:**
> Failover 중 4~5분 동안 DB 연결이 끊기므로, 백엔드에서 DB 연결 실패 시 5초 간격 3회 retry 로직 필요.

**현재 가이드:** HikariCP `connection-timeout: 30000`만 언급. Failover 중 연결이 끊기는 시간(최대 5분) 대비 재연결 전략이 없다.

**추가 필요 (application-prod.yaml):**
```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 300000    # Failover 5분 커버 (기존 30초 → 5분)
      initialization-fail-timeout: -1  # 시작 시 DB 없어도 계속 재시도
      keepalive-time: 30000
```

---

### 2-6. 도쿄 리전 VPC / 보안그룹 / 서브넷 그룹 설정 누락

현재 가이드는 서울 리전 네트워크 설정만 있다. 도쿄 리전에도 동일한 구성이 필요하다:
- `once-aurora-sg-tokyo` (도쿄 EKS SG → 5432)
- `once-aurora-subnet-group-tokyo` (도쿄 Private Subnet 2개)
- 도쿄 파라미터 그룹 (timezone: Asia/Tokyo 또는 UTC 통일)

---

### 2-7. EKS 환경의 Secrets 주입 방식 누락

**현재 가이드:** systemd EnvironmentFile, EC2 User Data 기반

**EKS 환경 실제 방법:**
```yaml
# Kubernetes Secret (또는 External Secrets Operator)
apiVersion: v1
kind: Secret
metadata:
  name: once-db-secret
type: Opaque
stringData:
  DB_URL: "jdbc:postgresql://<GLOBAL_WRITER_ENDPOINT>:5432/once_db"
  DB_USERNAME: "onceapp"
  DB_PASSWORD: "<from-secrets-manager>"
---
# Deployment에서 참조
envFrom:
  - secretRef:
      name: once-db-secret
```

---

## 3. 정확한 항목 (✅ PASS)

| 항목 | 내용 | 판정 |
|---|---|---|
| 보안그룹 구성 | 퍼블릭 오픈 금지, SG ID 기반 인바운드 | ✅ |
| DB 서브넷 그룹 | Private Subnet 2개 이상, AZ 분리 | ✅ |
| DROP 문 제거 | 운영 환경에서 DROP 실행 금지, grep -v 방법 | ✅ |
| 전용 DB 유저 | `oncedba`(관리)와 `onceapp`(앱) 분리, 최소 권한 | ✅ |
| KMS 암호화 | 저장 데이터 암호화 활성화 | ✅ |
| Secrets Manager | DB 자격증명 Secrets Manager 보관 | ✅ |
| 파라미터 그룹 | timezone, log_min_duration_statement | ✅ |
| 백업 보존 7일 | 자동 백업 + 수동 스냅샷 | ✅ |
| ddl-auto: validate | 스키마 불일치 시 앱 기동 실패로 사전 감지 | ✅ |

---

## 4. 가이드 수정 우선순위 로드맵

| 우선순위 | 수정 항목 | 예상 작업량 |
|---|---|---|
| P0 | 아키텍처 다이어그램을 Global Database + EKS 구조로 교체 | 소 |
| P0 | 인스턴스 타입 `db.t3.medium` → `db.t4g.medium` 전체 교체 | 소 |
| P0 | Aurora Global Database 생성 섹션 추가 (도쿄 Secondary 포함) | 대 |
| P0 | Failover API를 `failover-global-cluster`로 교체 | 소 |
| P0 | RTO/RPO 수치를 의사결정 문서 기준으로 교체 | 소 |
| P1 | Route 53 Health Check + Step Functions 자동화 섹션 추가 | 대 |
| P1 | Failback 3단계 절차 섹션 추가 | 중 |
| P1 | 데이터 정합성 검증 SQL + 자동화 섹션 추가 | 중 |
| P2 | Spring Boot Failover 재연결 설정 (`connection-timeout` 수정) | 소 |
| P2 | EKS Kubernetes Secret 주입 방식으로 섹션 교체 | 소 |
| P2 | 도쿄 리전 VPC/SG/서브넷 설정 섹션 추가 | 중 |
| P3 | AuroraGlobalDBReplicationLag CloudWatch Alarm 추가 | 소 |

---

## 5. 핵심 판단 요약

현재 `aurora-setup-guide.md`는 **단일 리전 Aurora 클러스터를 처음 구축하는 일반 가이드**로서는 완성도가 높다. 그러나 이 프로젝트의 핵심은 다음 세 가지다:

1. **Aurora Global Database** — 단순 Aurora 클러스터가 아님
2. **Cross-Region Failover/Failback 자동화** — 수동 명령어가 아닌 Step Functions 파이프라인
3. **RPO/RTO 수치 실측 및 증명** — `AuroraGlobalDBReplicationLag` 메트릭이 핵심 증거

이 세 가지가 현재 가이드에 모두 빠져 있다. 현재 가이드대로 구축하면 서울 단일 클러스터만 존재하고 도쿄 DR이 없어 프로젝트 성공 기준을 달성할 수 없다.
