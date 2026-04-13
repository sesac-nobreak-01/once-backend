package com.once.globalnews.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.once.globalnews.global.common.exception.JwtTokenAuthenticationException;
import com.once.globalnews.global.security.jwt.JwtTokenProvider;
import com.once.globalnews.global.security.util.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenProvider jwtUtil;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) throws ServletException {
        var path = req.getRequestURI();
        var shouldSkip = SecurityConstants.ALLOW_URLS.stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, path));

        log.debug("Should skip JwtFilter for path {}: {}", path, shouldSkip);
        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = resolveAccessToken(request);

        try {
            if (accessToken != null) {
                if (jwtUtil.validateToken(accessToken)) {
                    SecurityContextHolder.getContext()
                            .setAuthentication(jwtUtil.getAuthentication(accessToken));
                } else {
                    log.debug("invalid accessToken: {}", accessToken);
                    throw new JwtTokenAuthenticationException("invalid accessToken");
                }
            } else {
                throw new JwtTokenAuthenticationException("Access Token not exist");
            }
        } catch (JwtTokenAuthenticationException e) {
            request.setAttribute("exception", e);
        } catch (Exception e) {
            log.error("[JwtFilter] Unexpected error during token processing: {}", e.getMessage());
            request.setAttribute("exception", new JwtTokenAuthenticationException(e.getMessage()));
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.split(" ")[1];
        }

        return null;
    }

}


