# AI 채팅 API 문서

## 개요
AWS Bedrock의 Claude 모델을 활용한 AI 채팅 기능입니다. 뉴스 기사에 대한 질문과 답변을 처리하며, 채팅 세션과 메시지를 저장합니다.

## 환경 설정

### 필수 환경 변수
```bash
# AWS Bedrock 설정
AWS_REGION=us-east-1  # AWS Bedrock 리전
AWS_ACCESS_KEY_ID=your_access_key  # AWS Access Key
AWS_SECRET_ACCESS_KEY=your_secret_key  # AWS Secret Key

# 기존 환경 변수
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=your_jwt_secret
JWT_ACCESS_TOKEN_VALIDITY_IN_SECONDS=86400
JWT_REFRESH_TOKEN_VALIDITY_IN_SECONDS=604800
REST_API_KEY=your_kakao_api_key
KAKAO_REDIRECT_URI=http://localhost:3000/auth/kakao/callback
```

### AWS Bedrock 권한 설정
AWS IAM 사용자에 다음 권한이 필요합니다:
- `bedrock:InvokeModel` - Claude 모델 호출 권한
- `bedrock:ListFoundationModels` - 모델 목록 조회 권한

## API 엔드포인트

### 1. 채팅 세션 생성
```http
POST /api/v1/chat/sessions
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "newsId": "news-123",
  "newsTitle": "EU Passes Comprehensive AI Regulation Act",
  "newsContent": "뉴스 본문 내용...",
  "newsUrl": "https://example.com/news/123",
  "firstMessage": "이 기사를 한국어로 요약해줘"  // 선택사항
}
```

**응답:**
```json
{
  "httpStatus": "CREATED",
  "code": "CHAT201",
  "message": "채팅 세션이 생성되었습니다.",
  "result": {
    "sessionId": 1,
    "title": "EU AI 규제법안 요약",
    "summary": "이 기사를 한국어로 요약해줘",
    "newsId": "news-123",
    "newsTitle": "EU Passes Comprehensive AI Regulation Act",
    "createdAt": "2026-04-14T10:30:00"
  }
}
```

### 2. 메시지 전송
```http
POST /api/v1/chat/sessions/{sessionId}/messages
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "message": "핵심 포인트를 3줄로 정리해줘"
}
```

**응답:**
```json
{
  "httpStatus": "OK",
  "code": "CHAT200",
  "message": "메시지가 전송되었습니다.",
  "result": {
    "sessionId": 1,
    "message": "EU AI 규제법안의 핵심 포인트는 다음과 같습니다:\n\n1. 고위험 AI 시스템에 대한 엄격한 규제...\n2. 생체 인식 기술 사용 제한...\n3. 투명성 의무 강화...",
    "messageType": "assistant"
  }
}
```

### 3. 채팅 세션 목록 조회
```http
GET /api/v1/chat/sessions?page=0&size=10
Authorization: Bearer {access_token}
```

**응답:**
```json
{
  "httpStatus": "OK",
  "code": "CHAT200",
  "message": "채팅 세션 목록을 조회했습니다.",
  "result": {
    "content": [
      {
        "sessionId": 1,
        "title": "EU AI 규제법안 요약",
        "summary": "이 기사를 한국어로 요약해줘",
        "newsId": "news-123",
        "newsTitle": "EU Passes Comprehensive AI Regulation Act",
        "createdAt": "2026-04-14T10:30:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

### 4. 채팅 세션 상세 조회
```http
GET /api/v1/chat/sessions/{sessionId}
Authorization: Bearer {access_token}
```

**응답:**
```json
{
  "httpStatus": "OK",
  "code": "CHAT200",
  "message": "채팅 세션 상세를 조회했습니다.",
  "result": {
    "sessionId": 1,
    "title": "EU AI 규제법안 요약",
    "summary": "이 기사를 한국어로 요약해줘",
    "newsId": "news-123",
    "newsTitle": "EU Passes Comprehensive AI Regulation Act",
    "messages": [
      {
        "messageId": 1,
        "content": "이 기사를 한국어로 요약해줘",
        "messageType": "USER",
        "createdAt": "2026-04-14T10:30:00"
      },
      {
        "messageId": 2,
        "content": "EU가 포괄적인 AI 규제법안을 통과시켰습니다...",
        "messageType": "ASSISTANT",
        "createdAt": "2026-04-14T10:30:05"
      }
    ],
    "createdAt": "2026-04-14T10:30:00"
  }
}
```

### 5. 채팅 세션 삭제
```http
DELETE /api/v1/chat/sessions/{sessionId}
Authorization: Bearer {access_token}
```

**응답:**
```json
{
  "httpStatus": "NO_CONTENT",
  "code": "CHAT204",
  "message": "채팅 세션이 삭제되었습니다.",
  "result": null
}
```

## 에러 코드

| 코드 | HTTP Status | 설명 |
|------|------------|------|
| CHAT404 | 404 | 채팅 세션을 찾을 수 없습니다 |
| CHAT503 | 503 | AI 서비스가 일시적으로 사용할 수 없습니다 |
| CHAT400 | 400 | 잘못된 채팅 요청입니다 |

## 주요 기능

### 1. 뉴스 컨텍스트 기반 대화
- 뉴스 기사 정보를 AI에게 제공하여 관련 질문에 정확한 답변
- 기사 요약, 번역, 핵심 포인트 정리 등

### 2. 대화 내역 저장
- 모든 채팅 세션과 메시지를 데이터베이스에 저장
- 사용자별로 독립적인 채팅 내역 관리

### 3. 자동 제목 생성
- 첫 메시지를 기반으로 채팅 세션 제목 자동 생성
- 대화 내용을 요약한 간결한 제목 제공

### 4. 스마트 프롬프트
- 시스템 프롬프트로 AI 역할 정의
- 뉴스 분석, 요약, 번역 등에 최적화된 응답 생성

## 테스트 시나리오

### 시나리오 1: 뉴스 기사 요약
1. 뉴스 정보와 함께 채팅 세션 생성
2. "이 기사를 한국어로 요약해줘" 메시지 전송
3. AI가 기사를 분석하여 한국어 요약 제공

### 시나리오 2: 핵심 포인트 정리
1. 기존 세션에서 "핵심 포인트 3줄 정리" 요청
2. AI가 기사의 핵심 내용을 3개 포인트로 정리

### 시나리오 3: 배경 설명 요청
1. "이 기사의 배경을 설명해줘" 메시지 전송
2. AI가 관련 배경 지식과 컨텍스트 제공

## 프론트엔드 연동 가이드

### API 클라이언트 예시
```typescript
// 채팅 세션 생성
const createChatSession = async (newsData: NewsData) => {
  const response = await fetch('/api/v1/chat/sessions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      newsId: newsData.id,
      newsTitle: newsData.title,
      newsContent: newsData.content,
      newsUrl: newsData.url
    })
  });
  return response.json();
};

// 메시지 전송
const sendMessage = async (sessionId: number, message: string) => {
  const response = await fetch(`/api/v1/chat/sessions/${sessionId}/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({ message })
  });
  return response.json();
};
```

## 주의사항

1. **AWS 비용**: Bedrock API 호출 시 토큰 단위로 비용이 발생합니다
2. **Rate Limiting**: 과도한 요청을 방지하기 위해 사용자별 요청 제한 고려
3. **컨텍스트 길이**: 긴 대화의 경우 컨텍스트 길이 제한 고려 (최대 2000 토큰)
4. **응답 시간**: AI 모델 응답에 1-3초 정도 소요될 수 있음