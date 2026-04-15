-- PostgreSQL Schema for Once Global News Application
-- Date: 2026-04-14

-- Drop existing tables if they exist (be careful in production!)
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS chat_rooms CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Create users table
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    kakao_id BIGINT NOT NULL UNIQUE,
    email VARCHAR(255),
    nickname VARCHAR(50) NOT NULL,
    profile_image VARCHAR(255),
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