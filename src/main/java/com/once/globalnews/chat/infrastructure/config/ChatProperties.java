package com.once.globalnews.chat.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 채팅 관련 설정 프로퍼티
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    /**
     * Rate Limiting 설정
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 메시지 제한 설정
     */
    private MessageLimit messageLimit = new MessageLimit();

    /**
     * AI 설정
     */
    private AI ai = new AI();

    @Getter
    @Setter
    public static class RateLimit {
        /**
         * 일일 최대 호출 횟수
         */
        private int dailyLimit = 20;

        /**
         * Rate limit 초과 시 재시도 가능 시간 (시간 단위)
         */
        private int resetHours = 24;

        /**
         * Redis 키 TTL (초)
         */
        private long ttlSeconds = 86400; // 24시간
    }

    @Getter
    @Setter
    public static class MessageLimit {
        /**
         * 세션당 최대 메시지 수
         */
        private int maxMessagesPerSession = 100;

        /**
         * 사용자당 최대 세션 수
         */
        private int maxSessionsPerUser = 50;

        /**
         * 히스토리에 포함할 최근 메시지 수
         */
        private int historyLimit = 10;

        /**
         * 메시지 최대 길이
         */
        private int maxMessageLength = 2000;
    }

    @Getter
    @Setter
    public static class AI {
        /**
         * AI 응답 타임아웃 (초)
         */
        private int responseTimeout = 30;

        /**
         * 최대 토큰 수
         */
        private int maxTokens = 2000;

        /**
         * 재시도 횟수
         */
        private int maxRetries = 2;
    }
}