package com.once.globalnews.chat.presentation.exception;

import com.once.globalnews.global.common.exception.ServiceException;
import lombok.Getter;

/**
 * AI 채팅 Rate Limit 초과 예외
 */
@Getter
public class ChatRateLimitException extends ServiceException {

    private final int currentCount;
    private final int dailyLimit;
    private final long hoursUntilReset;

    public ChatRateLimitException(String message, int currentCount, int dailyLimit, long hoursUntilReset) {
        super("CHAT_RATE_LIMIT", message);
        this.currentCount = currentCount;
        this.dailyLimit = dailyLimit;
        this.hoursUntilReset = hoursUntilReset;
    }

    public String getDetailMessage() {
        return String.format("현재 사용: %d/%d회, %d시간 후 초기화",
                currentCount, dailyLimit, hoursUntilReset);
    }
}