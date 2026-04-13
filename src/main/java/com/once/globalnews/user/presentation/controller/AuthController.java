package com.once.globalnews.user.presentation.controller;

import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.SuccessStatus;
import com.once.globalnews.global.security.annotation.GlobalNewsUser;
import com.once.globalnews.global.security.jwt.JwtTokenProvider;
import com.once.globalnews.global.security.jwt.RefreshTokenService;
import com.once.globalnews.global.security.util.AuthCookieFactory;
import com.once.globalnews.user.application.AuthService;
import com.once.globalnews.user.application.UserService;
import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.presentation.model.response.AccessTokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieFactory authCookieFactory;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    @GetMapping("/callback")
    public ApiResponse<AccessTokenResponse> callback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) {
        log.info("[AuthController] callback called, code: {}", code);
        User user = authService.kakaoLoginUser(code);
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);

        refreshTokenService.store(newRefreshToken, user.getId(), Duration.ofSeconds(refreshTokenValidityInSeconds));
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.buildRefreshCookie(newRefreshToken, refreshTokenValidityInSeconds).toString());

        boolean isFirstLogin = "globalnews-new-kakao-user".equals(user.getNickname());
        return ApiResponse.onSuccess(
                SuccessStatus.USER_KAKAO_LOGIN_SUCCESS,
                new AccessTokenResponse(newAccessToken, isFirstLogin));
    }

    @GetMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public String test(@GlobalNewsUser User user) {
        return user.getNickname();
    }
}
