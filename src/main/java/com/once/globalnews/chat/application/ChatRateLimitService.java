package com.once.globalnews.chat.application;

import com.once.globalnews.chat.infrastructure.config.ChatProperties;
import com.once.globalnews.chat.presentation.exception.ChatRateLimitException;
import com.once.globalnews.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * AI 채팅 Rate Limiting 서비스
 * Redis를 활용하여 사용자별 일일 호출 횟수 제한
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatProperties chatProperties;

    private static final String RATE_LIMIT_KEY_PREFIX = "chat:rate_limit:";
    private static final String DAILY_KEY_SUFFIX = ":daily";

    /**
     * Rate limit 체크 및 카운트 증가
     * @param user 사용자
     * @return 현재 사용 횟수
     */
    public void checkAndIncrementRateLimit(User user) {
        String key = getRateLimitKey(user.getId());
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 현재 카운트 조회
        String currentCountStr = ops.get(key);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        int dailyLimit = chatProperties.getRateLimit().getDailyLimit();

        // 제한 초과 체크
        if (currentCount >= dailyLimit) {
            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            long hoursUntilReset = ttl > 0 ? (ttl / 3600) + 1 : chatProperties.getRateLimit().getResetHours();

            log.warn("Rate limit exceeded for user: {} (current: {}/{})",
                    user.getId(), currentCount, dailyLimit);

            throw new ChatRateLimitException(
                    String.format("일일 AI 채팅 한도(%d회)를 초과했습니다. %d시간 후에 다시 시도해주세요.",
                            dailyLimit, hoursUntilReset),
                    currentCount,
                    dailyLimit,
                    hoursUntilReset
            );
        }

        // 카운트 증가
        Long newCount = ops.increment(key);

        // 첫 호출인 경우 TTL 설정 (자정까지)
        if (newCount == 1) {
            long secondsUntilMidnight = getSecondsUntilMidnight();
            redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
            log.info("Rate limit key created for user: {} (TTL: {}s)", user.getId(), secondsUntilMidnight);
        }

        log.info("Rate limit incremented for user: {} ({}/{})",
                user.getId(), newCount, dailyLimit);
    }

    /**
     * 사용자의 현재 사용 횟수 조회
     * @param userId 사용자 ID
     * @return 현재 사용 횟수
     */
    public int getCurrentUsageCount(Long userId) {
        String key = getRateLimitKey(userId);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 사용자의 남은 사용 횟수 조회
     * @param userId 사용자 ID
     * @return 남은 사용 횟수
     */
    public int getRemainingCount(Long userId) {
        int currentCount = getCurrentUsageCount(userId);
        int dailyLimit = chatProperties.getRateLimit().getDailyLimit();
        return Math.max(0, dailyLimit - currentCount);
    }

    /**
     * Rate limit 초기화 (테스트/관리용)
     * @param userId 사용자 ID
     */
    public void resetRateLimit(Long userId) {
        String key = getRateLimitKey(userId);
        redisTemplate.delete(key);
        log.info("Rate limit reset for user: {}", userId);
    }

    /**
     * Rate limit 정보 조회
     * @param userId 사용자 ID
     * @return Rate limit 정보
     */
    public RateLimitInfo getRateLimitInfo(Long userId) {
        int currentCount = getCurrentUsageCount(userId);
        int dailyLimit = chatProperties.getRateLimit().getDailyLimit();
        int remainingCount = Math.max(0, dailyLimit - currentCount);

        String key = getRateLimitKey(userId);
        long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long hoursUntilReset = ttl > 0 ? (ttl / 3600) + 1 : 0;

        return new RateLimitInfo(currentCount, remainingCount, dailyLimit, hoursUntilReset);
    }

    /**
     * Rate limit Redis 키 생성
     */
    private String getRateLimitKey(Long userId) {
        String date = LocalDate.now().toString();
        return RATE_LIMIT_KEY_PREFIX + userId + ":" + date + DAILY_KEY_SUFFIX;
    }

    /**
     * 자정까지 남은 초 계산
     */
    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    /**
     * Rate limit 정보 DTO
     */
    public record RateLimitInfo(
            int currentCount,
            int remainingCount,
            int dailyLimit,
            long hoursUntilReset
    ) {
        public boolean isExceeded() {
            return remainingCount <= 0;
        }
    }
}