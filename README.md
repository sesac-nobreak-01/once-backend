# Once Global News Backend

## 프로젝트 개요
글로벌 뉴스 플랫폼의 백엔드 서비스로, 실시간 뉴스 수집, AI 기반 채팅 서비스, 사용자 관리 등의 기능을 제공합니다.

## 주요 기능

### 1. 사용자 관리
- 카카오 소셜 로그인
- JWT 기반 인증/인가
- 사용자 프로필 관리 (국가 설정 등)

### 2. AI 채팅 서비스
- Amazon Bedrock (Claude) 기반 AI 어시스턴트
- 뉴스 기사 컨텍스트 기반 대화
- 일일 사용량 제한 (100회)
- 대화 히스토리 관리

### 3. 뉴스 수집 시스템 (예정)
- GNews API를 통한 자동 뉴스 수집
- 다국어 뉴스 지원
- 카테고리별 뉴스 분류

## 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.3.5**
- **Spring Security** - 인증/인가
- **Spring Data JPA** - ORM

### Database
- **PostgreSQL** - 메인 데이터베이스
- **Redis** - 캐싱 및 Rate Limiting

### AI/ML
- **Amazon Bedrock** - Claude Sonnet 모델
- **Spring AI** - AI 통합

### Infrastructure
- **Docker** - 컨테이너화
- **AWS** - 클라우드 인프라

## 프로젝트 구조

```
src/main/java/com/once/globalnews/
├── chat/                    # AI 채팅 기능
│   ├── application/        # 비즈니스 로직
│   ├── domain/            # 도메인 모델
│   ├── infrastructure/    # 외부 서비스 연동
│   └── presentation/      # API 엔드포인트
├── user/                    # 사용자 관리
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
├── news/                    # 뉴스 관리 (개발 예정)
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
└── global/                  # 공통 설정
    ├── common/            # 공통 응답/예외
    ├── config/            # 설정
    └── security/          # 보안
```

## API 엔드포인트

### 인증
- `POST /api/v1/auth/kakao` - 카카오 로그인
- `POST /api/v1/auth/refresh` - 토큰 갱신

### 사용자
- `GET /api/v1/user` - 사용자 정보 조회
- `DELETE /api/v1/user` - 회원 탈퇴
- `PATCH /api/v1/user/country` - 국가 설정 변경

### AI 채팅
- `POST /api/v1/chat/sessions` - 채팅 세션 생성
- `GET /api/v1/chat/sessions` - 채팅 세션 목록
- `GET /api/v1/chat/sessions/{id}` - 채팅 세션 상세
- `POST /api/v1/chat/sessions/{id}/messages` - 메시지 전송
- `DELETE /api/v1/chat/sessions/{id}` - 채팅 세션 삭제
- `GET /api/v1/chat/rate-limit` - 사용량 조회

### 뉴스 (개발 예정)
- `GET /api/v1/news` - 뉴스 목록
- `GET /api/v1/news/{id}` - 뉴스 상세
- `GET /api/v1/news/categories` - 카테고리별 뉴스
- `GET /api/v1/news/search` - 뉴스 검색

## 환경 설정

### 필수 환경 변수

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/once_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-secret-key
JWT_ACCESS_TOKEN_VALIDITY_IN_SECONDS=3600
JWT_REFRESH_TOKEN_VALIDITY_IN_SECONDS=2592000

# Kakao OAuth
REST_API_KEY=your-kakao-rest-api-key
KAKAO_REDIRECT_URI=http://localhost:3000/auth/kakao/callback

# AWS Bedrock
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
BEDROCK_MODEL_ID=anthropic.claude-3-sonnet-20240229-v1:0

# GNews API (예정)
GNEWS_API_KEY=your-gnews-api-key
GNEWS_API_URL=https://gnews.io/api/v4
```

## 실행 방법

### 1. 데이터베이스 설정
```bash
# PostgreSQL 실행 (Docker)
docker run -d \
  --name postgres \
  -e POSTGRES_DB=once_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

# Redis 실행 (Docker)
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7
```

### 2. 애플리케이션 실행
```bash
# 개발 환경
./gradlew bootRun

# 빌드
./gradlew clean build

# JAR 실행
java -jar build/libs/globalnews-0.0.1-SNAPSHOT.jar
```

## 뉴스 수집 시스템 아키텍처 (제안)

### 옵션 1: 동일 서버 내 스케줄러
- **장점**:
  - 단순한 구조
  - 운영 관리 용이
  - 트랜잭션 관리 간단
- **단점**:
  - 메인 서비스 성능 영향 가능
  - 스케일링 어려움
- **구현**:
  - Spring `@Scheduled` 사용
  - 10분 간격 실행

### 옵션 2: 별도 마이크로서비스 (추천)
- **장점**:
  - 독립적인 스케일링
  - 메인 서비스 영향 없음
  - 장애 격리
- **단점**:
  - 복잡도 증가
  - 별도 배포/관리 필요
- **구현**:
  - 별도 Spring Boot 애플리케이션
  - 동일 DB 접근 또는 API 통신
  - Kubernetes CronJob 활용 가능

### 옵션 3: AWS Lambda + EventBridge
- **장점**:
  - 서버리스로 비용 효율적
  - 자동 스케일링
  - 관리 부담 최소화
- **단점**:
  - AWS 종속성
  - 콜드 스타트 이슈
- **구현**:
  - EventBridge로 10분 간격 트리거
  - Lambda에서 GNews API 호출 및 DB 저장

### 추천 구현 방안

**단기 (MVP)**: 옵션 1 - 동일 서버 스케줄러
```java
@Component
public class NewsCollectorScheduler {
    @Scheduled(fixedDelay = 600000) // 10분
    public void collectNews() {
        // GNews API 호출
        // 뉴스 데이터 저장
    }
}
```

**장기 (확장)**: 옵션 2 - 별도 마이크로서비스
- 독립적인 뉴스 수집 서비스
- Message Queue (RabbitMQ/Kafka) 활용
- 이벤트 기반 아키텍처

## 뉴스 수집 주기 설정

### 제안하는 수집 주기
- **Breaking News**: 5분 간격 (주요 키워드 기반)
- **General News**: 30분 간격 (카테고리별)
- **Archive News**: 1시간 간격 (과거 뉴스 업데이트)

### Rate Limit 고려사항
- GNews API 일일 제한 확인 필요
- 캐싱 전략 수립
- 중복 뉴스 필터링

## 개발 로드맵

### Phase 1: MVP (현재)
- [x] 사용자 인증/인가
- [x] AI 채팅 서비스
- [ ] 기본 뉴스 수집 (수동)

### Phase 2: 자동화
- [ ] 뉴스 자동 수집 스케줄러
- [ ] 뉴스 카테고리 분류
- [ ] 다국어 지원

### Phase 3: 고도화
- [ ] 개인화 뉴스 추천
- [ ] 실시간 알림
- [ ] 뉴스 요약 기능

## 테스트

```bash
# 단위 테스트
./gradlew test

# 통합 테스트
./gradlew integrationTest

# 테스트 커버리지
./gradlew jacocoTestReport
```

## 모니터링

- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Swagger UI**: `/swagger-ui.html`

## 기여 방법

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 라이센스

This project is licensed under the MIT License.

## 문의

- **Email**: support@once.com
- **Issue Tracker**: GitHub Issues