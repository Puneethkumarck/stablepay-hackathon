package com.stablepay.testutil;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.stablepay.application.config.AppUserConverter;
import com.stablepay.application.config.SecurityAuthenticationEntryPoint;
import com.stablepay.application.config.SecurityConfig;

@TestConfiguration
@Import({TestClockConfig.class, SecurityConfig.class, AppUserConverter.class, SecurityAuthenticationEntryPoint.class})
public class TestSecurityConfig {

    @Bean
    public JwtDecoder appJwtDecoder() {
        var secretKey = new SecretKeySpec(
                AuthFixtures.SOME_JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
