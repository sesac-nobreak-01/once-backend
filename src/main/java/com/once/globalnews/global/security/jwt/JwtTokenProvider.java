package com.once.globalnews.global.security.jwt;

import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.infrastructure.persistence.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    private final UserRepository userRepository;

    private static final String CLAIM_ID = "id";
    private static final String CLAIM_TYP = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity-in-seconds}")
    private long accessTokenValidityInSeconds;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    private JwtParser jwtParser;
    private SecretKey signingKey;

    @PostConstruct
    protected void init() {
        var keyBytes = Decoders.BASE64.decode(secretKey);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .build();
    }

    public String createAccessToken(User user) {
        return createToken(user, accessTokenValidityInSeconds, TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(User user) {
        return createToken(user, refreshTokenValidityInSeconds, TOKEN_TYPE_REFRESH);
    }

    private String createToken(User user, long validityInSeconds, String tokenType) {
        var now = new Date();
        var validity = new Date(now.getTime() + validityInSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_ID, user.getId())
                .claim(CLAIM_TYP, tokenType)
                .issuedAt(now)
                .expiration(validity)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            var claims = jwtParser.parseSignedClaims(token)
                    .getPayload();
            assertAccessTokenType(claims);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    public Long parseRefreshToken(String token) {
        var claims = jwtParser.parseSignedClaims(token)
                .getPayload();
        assertRefreshTokenType(claims);
        return getUserId(claims);
    }

    public Authentication getAuthentication(String token) {
        var claims = jwtParser.parseSignedClaims(token)
                .getPayload();
        assertAccessTokenType(claims);
        var userId = getUserId(claims);

        var user = userRepository.findById(userId).orElseThrow(ErrorStatus.NOT_AUTHORIZED::serviceException);
        var principal = CustomUserDetails.from(user);

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
        // return new UsernamePasswordAuthenticationToken(principal, token);
    }

    public Long parseAccessToken(String token) {
        var claims = jwtParser.parseSignedClaims(token)
                .getPayload();

        assertAccessTokenType(claims);
        return getUserId(claims);
    }

    private Long getUserId(Claims claims) {
        var subject = claims.getSubject();
        if (subject != null && !subject.isBlank()) {
            return Long.parseLong(subject);
        }
        return claims.get(CLAIM_ID, Long.class);
    }

    private void assertAccessTokenType(Claims claims) {
        var tokenType = claims.get(CLAIM_TYP, String.class);
        if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
            throw ErrorStatus.NOT_AUTHORIZED.serviceException();
        }
    }

    private void assertRefreshTokenType(Claims claims) {
        var tokenType = claims.get(CLAIM_TYP, String.class);
        if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw ErrorStatus.NOT_AUTHORIZED.serviceException();
        }
    }
}

