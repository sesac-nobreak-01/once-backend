package com.once.globalnews.global.infrastructure.config;

import com.once.globalnews.global.security.handler.GlobalNewsUserArgumentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final GlobalNewsUserArgumentHandler globalNewsUserArgumentHandler;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers){
        argumentResolvers.add(globalNewsUserArgumentHandler);
    }
}
