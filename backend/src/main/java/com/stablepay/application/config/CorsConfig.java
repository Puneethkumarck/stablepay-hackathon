package com.stablepay.application.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var origins = corsProperties.allowedOrigins().toArray(String[]::new);
        var methods = corsProperties.allowedMethods().toArray(String[]::new);
        var headers = corsProperties.allowedHeaders().toArray(String[]::new);

        log.info("Configuring CORS with allowed origins: {}", corsProperties.allowedOrigins());

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods(methods)
                .allowedHeaders(headers)
                .maxAge(corsProperties.maxAge());
    }
}
