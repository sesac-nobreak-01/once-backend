package com.once.globalnews.user.application;

import com.once.globalnews.global.common.exception.ServiceException;
import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.global.security.jwt.JwtTokenProvider;
import com.once.globalnews.global.security.jwt.RefreshTokenService;
import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.infrastructure.persistence.UserRepository;
import com.once.globalnews.user.presentation.converter.UserConverter;
import com.once.globalnews.user.presentation.model.response.KakaoTokenResponse;
import com.once.globalnews.user.presentation.model.response.KakaoUserInfoResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    @Value("${kakao.client_id}")
    private String clientId;

    @Value("${kakao.redirect_uri}")
    private String redirectUri;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    private final String KAUTH_TOKEN_URL_HOST = "https://kauth.kakao.com";
    private final String KAUTH_USER_URL_HOST = "https://kapi.kakao.com";

    public String getAccessTokenFromKakao(String code) {

        KakaoTokenResponse kakaoTokenResponse = WebClient.create(KAUTH_TOKEN_URL_HOST).post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .path("/oauth/token")
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", clientId)
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("code", code)
                        .build())
                .header(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("[Kakao] getAccessToken 4xx error body: {}", body);
                            return Mono.error(new ServiceException(ErrorStatus.INVALID_PARAMETER.name(), ErrorStatus.INVALID_PARAMETER.getMessage()));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new ServiceException(ErrorStatus.INTERNAL_SERVER_ERROR.name(), ErrorStatus.INTERNAL_SERVER_ERROR.getMessage())))
                .bodyToMono(KakaoTokenResponse.class)
                .block();


        log.info(" [Kakao Service] Access Token ------> {}", kakaoTokenResponse.getAccessToken());
        log.info(" [Kakao Service] Refresh Token ------> {}", kakaoTokenResponse.getRefreshToken());
        //제공 조건: OpenID Connect가 활성화 된 앱의 토큰 발급 요청인 경우 또는 scope에 openid를 포함한 추가 항목 동의 받기 요청을 거친 토큰 발급 요청인 경우
        log.info(" [Kakao Service] Id Token ------> {}", kakaoTokenResponse.getIdToken());
        log.info(" [Kakao Service] Scope ------> {}", kakaoTokenResponse.getScope());

        return kakaoTokenResponse.getAccessToken();
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {

        log.info("[Kakao Service] Calling getUserInfo with accessToken: {}", accessToken);
        KakaoUserInfoResponse userInfo = WebClient.create(KAUTH_USER_URL_HOST)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .path("/v2/user/me")
                        .build(true))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("[Kakao] getUserInfo 4xx error body: {}", body);
                            return Mono.error(new ServiceException(ErrorStatus.INVALID_PARAMETER.name(), ErrorStatus.INVALID_PARAMETER.getMessage()));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new ServiceException(ErrorStatus.INTERNAL_SERVER_ERROR.name(), ErrorStatus.INTERNAL_SERVER_ERROR.getMessage())))
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();

        log.info("[ Kakao Service ] Auth ID ---> {} ", userInfo.getId());
        log.info("[ Kakao Service ] ProfileImageUrl ---> {} ", userInfo.getKakaoAccount().getProfile().getProfileImageUrl());

        return userInfo;
    }

    @Transactional
    public User kakaoLoginUser(String code) {
        String accessToken = getAccessTokenFromKakao(code);
        KakaoUserInfoResponse userInfo = getUserInfo(accessToken);
        return generateUserAfterKakaoAuth(userInfo);
    }

    @Transactional
    public User generateUserAfterKakaoAuth(KakaoUserInfoResponse userInfo) {
        Long kakaoId = userInfo.getId();

        String newProfileUrl = null;
        if (userInfo.getKakaoAccount().getProfile() != null) {
            newProfileUrl = userInfo.getKakaoAccount().getProfile().getProfileImageUrl();
        }

        // 카카오 ID로 기존 유저 조회
        User user = userRepository.findByKakaoId(kakaoId)

                .orElseGet(() -> {
                    // 없으면 회원가입
                    User newUser = UserConverter.userOf(userInfo);
                    return userRepository.save(newUser);
                });

        if (newProfileUrl != null
                && !newProfileUrl.isBlank()
                && !newProfileUrl.equals(user.getProfileImage())) {
            user.updateProfileImage(newProfileUrl);
        }

        return user;
    }

    public User validateAndConsumeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ErrorStatus.INVALID_TOKEN.serviceException();
        }

        Long tokenUserId = jwtTokenProvider.parseRefreshToken(refreshToken);
        Long storedUserId = refreshTokenService.getUserId(refreshToken);
        if (storedUserId == null || !storedUserId.equals(tokenUserId)) {
            throw ErrorStatus.INVALID_TOKEN.serviceException();
        }

        User user = userRepository.findById(storedUserId)
                .orElseThrow(ErrorStatus.NOT_AUTHORIZED::serviceException);

        refreshTokenService.delete(refreshToken);
        return user;
    }

    public String createAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    public String createRefreshTokenAndStore(User user) {
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        refreshTokenService.store(newRefreshToken, user.getId(), Duration.ofSeconds(refreshTokenValidityInSeconds));
        return newRefreshToken;
    }

    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenValidityInSeconds;
    }

    public boolean isFirstLogin(User user) {
        return "globalnews-new-kakao-user".equals(user.getNickname());
    }
}
