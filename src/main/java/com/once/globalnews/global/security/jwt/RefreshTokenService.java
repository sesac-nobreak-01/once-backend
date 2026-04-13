package com.once.globalnews.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "rt:";
    private final StringRedisTemplate redisTemplate;

    public void store(String refreshToken, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(key(refreshToken), userId.toString(), ttl);
    }

    public Long getUserId(String refreshToken) {
        String value = redisTemplate.opsForValue().get(key(refreshToken));
        return value == null ? null : Long.valueOf(value);
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(key(refreshToken));
    }

    private String key(String refreshToken) {
        return KEY_PREFIX + sha256Hex(refreshToken);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }
}

