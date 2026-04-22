# 뉴스 데이터 화면 미출력 원인 분석 보고서

## 요약

S3에 뉴스 데이터가 정상 적재되고 DB에도 메타데이터가 저장되어 있음에도 화면에 뉴스가 표시되지 않는 원인을 분석하였다.
핵심 원인은 **백엔드 API가 S3의 실제 기사 내용을 전혀 읽지 않고, title/content 같은 필수 필드를 응답에 포함하지 않기 때문**이다.

---

## 현재 데이터 흐름

```
Python 크론잡
  └── GNews API 뉴스 수집
  └── S3 저장: articles/raw/2026/04/21/{hash}.json  (title, content, source 등 전체 데이터)
  └── DB 저장: news_articles 테이블 (article_id, s3_url, full_content_s3_key, category, country, published_at 만 저장)

Spring Boot 백엔드
  └── GET /api/news → DB 조회 → NewsResponse 반환

NewsResponse 현재 반환 필드:
  - id
  - s3Url
  - category
  - country
  - publishedAt

프론트엔드
  └── /api/news 호출 → title, content 없이 s3Url만 받음 → 화면 표시 불가
```

---

## 문제 원인 상세

### 원인 1. NewsResponse에 title/content 필드 없음 (핵심)

**파일**: `news/presentation/model/response/NewsResponse.java`

```java
// 현재 상태
public class NewsResponse {
    private String id;
    private String s3Url;    // S3 URL만 반환
    private String category;
    private String country;
    private String publishedAt;
    // title, content, sourceName, imageUrl 없음
}
```

프론트엔드가 뉴스 카드를 렌더링하려면 최소한 `title`이 필요하지만 응답에 포함되지 않는다.

---

### 원인 2. 백엔드가 S3에서 기사 내용을 읽지 않음

**파일**: `news/application/NewsService.java`

`NewsService.getNews()`는 DB에서 메타데이터만 조회하고, `full_content_s3_key`를 이용해 S3에서 실제 JSON을 읽는 로직이 전혀 없다.

`S3ObjectMetadataReader.getObjectBytes()`가 존재하지만 채팅 첨부파일 용도로만 사용되고 뉴스에는 사용되지 않는다.

---

### 원인 3. news_articles 테이블에 title/content 컬럼 없음

**파일**: `src/main/resources/schema-postgresql.sql`

```sql
CREATE TABLE news_articles (
    id                  UUID PRIMARY KEY,
    article_id          VARCHAR(255) NOT NULL UNIQUE,
    s3_url              TEXT,
    full_content_s3_key VARCHAR(500),
    published_at        TIMESTAMP NOT NULL,
    category            VARCHAR(50) NOT NULL,
    country             VARCHAR(10) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    -- title, content, source_name, image_url 컬럼 없음
);
```

title 등의 필드가 DB 스키마 자체에 없다.

---

### 원인 4. news.s3.bucket 설정이 코드에서 미사용

**파일**: `src/main/resources/application.yaml`

```yaml
news:
  s3:
    bucket: ${NEWS_S3_BUCKET:global-news-ai}
```

설정은 있지만 이를 읽는 `@ConfigurationProperties` 클래스와 뉴스 전용 S3 서비스가 구현되어 있지 않다.

---

## 해결 방안

두 가지 방법 중 선택할 수 있다.

---

### 방법 A. DB에 title 등 필드 추가 (권장)

Python 크론잡이 DB에 저장할 때 title, source, imageUrl도 함께 저장하도록 확장한다.
백엔드는 S3를 읽을 필요 없이 DB 조회만으로 전체 응답을 구성할 수 있다.

**1. DB 스키마 변경** (`schema-postgresql.sql`)

```sql
ALTER TABLE news_articles
    ADD COLUMN title        TEXT,
    ADD COLUMN description  TEXT,
    ADD COLUMN source_name  VARCHAR(255),
    ADD COLUMN source_url   TEXT,
    ADD COLUMN image_url    TEXT,
    ADD COLUMN original_url TEXT;
```

**2. NewsArticle 엔티티 확장** (`news/domain/NewsArticle.java`)

```java
@Column(name = "title", columnDefinition = "TEXT")
private String title;

@Column(name = "description", columnDefinition = "TEXT")
private String description;

@Column(name = "source_name", length = 255)
private String sourceName;

@Column(name = "image_url", columnDefinition = "TEXT")
private String imageUrl;

@Column(name = "original_url", columnDefinition = "TEXT")
private String originalUrl;
```

**3. NewsResponse 확장** (`news/presentation/model/response/NewsResponse.java`)

```java
public class NewsResponse {
    private String id;
    private String title;
    private String description;
    private String sourceName;
    private String imageUrl;
    private String originalUrl;
    private String s3Url;
    private String category;
    private String country;
    private String publishedAt;
}
```

**4. Python 크론잡 수정** (`once-news-cronjob-main/src/services/news_service.py`)

`_save_article`에서 title, source, imageUrl 등을 DB에 함께 저장하도록 수정한다.

---

### 방법 B. 백엔드에서 S3 직접 읽기

뉴스 상세 조회 시 `full_content_s3_key`로 S3에서 JSON을 읽어 title, content를 파싱하여 반환한다.

**장점**: DB 스키마 변경 없음
**단점**: API 호출마다 S3 읽기 발생으로 응답 속도 저하, 비용 증가

---

## 권장 수정 순서

1. `once-news-cronjob-main` Python 크론잡의 `database.py` 모델에 title 등 컬럼 추가
2. `once-news-cronjob-main`의 `_save_article`에서 해당 필드 저장
3. `once-backend`의 `news_articles` 테이블에 컬럼 추가 (ALTER TABLE 또는 마이그레이션)
4. `NewsArticle` 엔티티에 필드 추가
5. `NewsResponse`에 필드 추가 및 `from()` 메서드 수정
6. 백엔드 재배포 후 Python 크론잡 재실행으로 새 데이터 적재

---

## 관련 파일 목록

| 파일 | 변경 필요 여부 |
|------|--------------|
| `news/domain/NewsArticle.java` | ✅ 필드 추가 |
| `news/presentation/model/response/NewsResponse.java` | ✅ 필드 추가 |
| `news/application/NewsService.java` | 방법 B 선택 시 수정 |
| `src/main/resources/schema-postgresql.sql` | ✅ 컬럼 추가 |
| `once-news-cronjob-main/src/models/database.py` | ✅ 컬럼 추가 |
| `once-news-cronjob-main/src/services/news_service.py` | ✅ 저장 필드 추가 |
