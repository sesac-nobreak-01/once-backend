package com.once.globalnews.global.security.handler;

import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.global.security.annotation.GlobalNewsUser;
import com.once.globalnews.global.security.jwt.JwtTokenProvider;
import com.once.globalnews.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class GlobalNewsUserArgumentHandler implements HandlerMethodArgumentResolver {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(GlobalNewsUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        var authentication = getAuthentication();

        if (authentication == null) {
            throw ErrorStatus.NOT_AUTHORIZED.serviceException();
        }

        var accessToken = authentication.getCredentials().toString();

        var userId = jwtTokenProvider.parseAccessToken(accessToken);

        return userRepository.findById(userId).orElseThrow(ErrorStatus.NO_PERMISSION::serviceException);
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal()
                .equals("anonymousUser")) {
            return null;
        }
        return authentication;
    }
}

