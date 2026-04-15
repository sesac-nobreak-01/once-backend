package com.once.globalnews.user.presentation.controller;

import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.SuccessStatus;
import com.once.globalnews.global.security.annotation.GlobalNewsUser;
import com.once.globalnews.global.security.jwt.JwtTokenProvider;
import com.once.globalnews.global.security.jwt.RefreshTokenService;
import com.once.globalnews.global.security.util.AuthCookieFactory;
import com.once.globalnews.user.application.AuthService;
import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.presentation.model.response.AccessTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
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

        return ApiResponse.onSuccess(
                SuccessStatus.USER_KAKAO_LOGIN_SUCCESS,
                new AccessTokenResponse(newAccessToken, authService.isFirstLogin(user), user.getNickname(), user.getProfileImage(), user.getPreferredCountry()));
    }

    @Operation(
            summary = "accessToken 재발급",
            description = "refreshToken으로 accessToken을 재발급 받습니다."
    )
    @PostMapping("/refresh")
    public ApiResponse<AccessTokenResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        User user = authService.validateAndConsumeRefreshToken(refreshToken);
        String newAccessToken = authService.createAccessToken(user);
        String newRefreshToken = authService.createRefreshTokenAndStore(user);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authCookieFactory.buildRefreshCookie(newRefreshToken, authService.getRefreshTokenValidityInSeconds()).toString()
        );
        return ApiResponse.onSuccess(
                SuccessStatus.OK,
                new AccessTokenResponse(newAccessToken, authService.isFirstLogin(user), user.getNickname(), user.getProfileImage(), user.getPreferredCountry())
        );
    }

    @Operation(
            summary = "로그아웃"
    )
    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.clearRefreshCookie().toString());
        return ApiResponse.onSuccess(SuccessStatus.OK, "logout");
    }

    @GetMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public String test(@GlobalNewsUser User user) {
        return user.getNickname();
    }

    @Operation(summary = "개발용 테스트 로그인", description = "임의의 카카오 ID로 유저를 생성하고 토큰을 발급합니다.")
    @GetMapping("/dev-login")
    public ApiResponse<AccessTokenResponse> devLogin(
            @RequestParam("kakaoId") Long kakaoId,
            HttpServletResponse response
    ) {
        log.info("[AuthController] devLogin called, kakaoId: {}", kakaoId);
        
        // NPE 방지를 위해 필요한 하위 객체들을 모두 생성
        com.once.globalnews.user.presentation.model.response.KakaoUserInfoResponse userInfo = 
            new com.once.globalnews.user.presentation.model.response.KakaoUserInfoResponse();
        userInfo.id = kakaoId;
        
        // KakaoAccount와 Profile 객체 생성 및 연결
        com.once.globalnews.user.presentation.model.response.KakaoUserInfoResponse.KakaoAccount account = 
            userInfo.new KakaoAccount();
        com.once.globalnews.user.presentation.model.response.KakaoUserInfoResponse.KakaoAccount.Profile profile = 
            account.new Profile();
        
        account.profile = profile;
        userInfo.kakaoAccount = account;
        
        // 가상의 닉네임 설정 (테스트 유저 구분을 위해)
        profile.nickName = "TestUser_" + kakaoId;
        
        User user = authService.generateUserAfterKakaoAuth(userInfo);
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);

        refreshTokenService.store(newRefreshToken, user.getId(), Duration.ofSeconds(refreshTokenValidityInSeconds));
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.buildRefreshCookie(newRefreshToken, refreshTokenValidityInSeconds).toString());

        return ApiResponse.onSuccess(
                SuccessStatus.USER_KAKAO_LOGIN_SUCCESS,
                new AccessTokenResponse(newAccessToken, authService.isFirstLogin(user), user.getNickname(), user.getProfileImage(), user.getPreferredCountry()));
    }
}
