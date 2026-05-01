package com.once.globalnews.global.security.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityConstants {
    public static final List<String> ALLOW_URLS = List.of(
            "/api/v1/auth/callback",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/dev-login",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/actuator/health",
            "/actuator/info",
            "/health"
    );

    /// 토큰이 있을 경우 인증 절차 진행하지만, 없을 경우에도 예외를 발생시키지는 않는 GET 메서드 URL
    public static final List<String> GET__METHOD_ALLOW_URLS = List.of(
            "/api/news",
            "/api/news/**"
    );
}
