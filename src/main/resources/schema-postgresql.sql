-- PostgreSQL Schema for Once Global News Application
-- Date: 2026-04-14

-- Drop existing tables if they exist (be careful in production!)
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS chat_attachments CASCADE;
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS chat_rooms CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS news_articles CASCADE;
DROP TABLE IF EXISTS news_sources CASCADE;

-- Create users table
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    kakao_id BIGINT NOT NULL UNIQUE,
    email VARCHAR(255),
    nickname VARCHAR(50) NOT NULL,
    profile_image VARCHAR(255),
    preferred_country VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create chat_rooms table (ChatSession in JPA)
CREATE TABLE chat_rooms (
    room_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    summary VARCHAR(500),
    article_id VARCHAR(100), -- news_id in JPA
    news_title VARCHAR(500),
    news_content TEXT,
    news_url VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_rooms_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- Create chat_messages table
CREATE TABLE chat_messages (
    message_id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    role VARCHAR(10) NOT NULL, -- USER, ASSISTANT, SYSTEM
    token_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_room
        FOREIGN KEY (room_id)
        REFERENCES chat_rooms(room_id)
        ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_users_kakao_id ON users(kakao_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_nickname ON users(nickname);
CREATE INDEX idx_chat_rooms_user_id ON chat_rooms(user_id);
CREATE INDEX idx_chat_rooms_article_id ON chat_rooms(article_id);
CREATE INDEX idx_chat_rooms_created_at ON chat_rooms(created_at DESC);
CREATE INDEX idx_chat_messages_room_id ON chat_messages(room_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);

-- Create chat_attachments table (file attachments for chat messages)
CREATE TABLE chat_attachments (
    attachment_id      BIGSERIAL PRIMARY KEY,
    public_id          UUID NOT NULL UNIQUE,
    user_id            BIGINT NOT NULL,
    room_id            BIGINT,
    message_id         BIGINT,
    s3_bucket          VARCHAR(128) NOT NULL,
    s3_key             VARCHAR(512) NOT NULL UNIQUE,
    original_filename  VARCHAR(255) NOT NULL,
    content_type       VARCHAR(128) NOT NULL,
    byte_size          BIGINT NOT NULL,
    checksum_sha256    VARCHAR(64),
    status             VARCHAR(20) NOT NULL,
    kind               VARCHAR(16) NOT NULL,
    extracted_text     TEXT,
    linked_at          TIMESTAMP,
    deleted_at         TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_attach_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_attach_room
        FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_attach_message
        FOREIGN KEY (message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_attach_user     ON chat_attachments(user_id);
CREATE INDEX idx_chat_attach_room     ON chat_attachments(room_id);
CREATE INDEX idx_chat_attach_message  ON chat_attachments(message_id);
CREATE INDEX idx_chat_attach_status   ON chat_attachments(status, created_at);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chat_rooms_updated_at BEFORE UPDATE ON chat_rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chat_messages_updated_at BEFORE UPDATE ON chat_messages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chat_attachments_updated_at BEFORE UPDATE ON chat_attachments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE users IS '사용자 정보 테이블';
COMMENT ON TABLE chat_rooms IS '채팅 세션(방) 정보 테이블';
COMMENT ON TABLE chat_messages IS '채팅 메시지 테이블';

COMMENT ON COLUMN users.kakao_id IS '카카오 OAuth 고유 ID';
COMMENT ON COLUMN users.role IS '사용자 권한 (USER, ADMIN 등)';
COMMENT ON COLUMN users.is_deleted IS '논리 삭제 여부';

COMMENT ON COLUMN chat_rooms.article_id IS '연관된 뉴스 기사 ID';
COMMENT ON COLUMN chat_rooms.is_active IS '채팅방 활성화 상태';

COMMENT ON COLUMN chat_messages.role IS '메시지 발신자 구분 (USER, ASSISTANT, SYSTEM)';
COMMENT ON COLUMN chat_messages.token_count IS 'AI 모델 토큰 사용량';

COMMENT ON TABLE chat_attachments IS '채팅 메시지 첨부파일 메타데이터 (블롭은 S3)';
COMMENT ON COLUMN chat_attachments.public_id IS '외부 노출용 UUID (S3 key 에 사용)';
COMMENT ON COLUMN chat_attachments.status IS 'PENDING_UPLOAD / UPLOADED / LINKED / DELETED / FAILED';
COMMENT ON COLUMN chat_attachments.kind IS 'IMAGE / DOCUMENT / TEXT / OTHER (LLM payload 분기)';
COMMENT ON COLUMN chat_attachments.s3_key IS 'S3 object key (chat/{userId}/{yyyy}/{MM}/{publicId}/{filename})';
COMMENT ON COLUMN chat_attachments.extracted_text IS 'Phase 3: Tika 로 추출한 텍스트 캐시';

-- Create news_sources table
CREATE TABLE news_sources (
    id          VARCHAR(255) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    url         TEXT,
    country     VARCHAR(10),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create news_articles table
CREATE TABLE news_articles (
    id                  UUID PRIMARY KEY,
    article_id          VARCHAR(255) NOT NULL UNIQUE,
    s3_url              TEXT,
    full_content_s3_key VARCHAR(500),
    published_at        TIMESTAMP NOT NULL,
    category            VARCHAR(50) NOT NULL,
    country             VARCHAR(10) NOT NULL,
    title               VARCHAR(500),
    description         TEXT,
    image_url           TEXT,
    original_url        TEXT,
    source_name         VARCHAR(255),
    source_url          TEXT,
    language            VARCHAR(10),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_articles_article_id  ON news_articles(article_id);
CREATE INDEX idx_news_articles_published_at ON news_articles(published_at DESC);
CREATE INDEX idx_news_articles_category    ON news_articles(category);
CREATE INDEX idx_news_articles_country     ON news_articles(country);

CREATE TRIGGER update_news_articles_updated_at BEFORE UPDATE ON news_articles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_news_sources_updated_at BEFORE UPDATE ON news_sources
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create reviews table
CREATE TABLE reviews (
    review_id  BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    rating     INTEGER     NOT NULL CHECK (rating >= 1 AND rating <= 5),
    content    TEXT,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_reviews_user_id    ON reviews(user_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE  reviews            IS 'AI 채팅 서비스 후기 테이블';
COMMENT ON COLUMN reviews.rating     IS '별점 (1~5)';
COMMENT ON COLUMN reviews.content    IS '후기 내용 (선택)';
COMMENT ON COLUMN reviews.user_id    IS '후기 작성자 (사용자당 1개 제한)';