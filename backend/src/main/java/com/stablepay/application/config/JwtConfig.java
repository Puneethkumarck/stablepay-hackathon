package com.stablepay.application.config;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.stablepay.domain.auth.model.AuthTokenConfig;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, GoogleAuthProps.class})
public class JwtConfig {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtProperties jwtProperties;
    private final GoogleAuthProps googleAuthProps;

    @Bean
    public AuthTokenConfig authTokenConfig() {
        return AuthTokenConfig.builder()
                .accessTtl(jwtProperties.accessTtl())
                .refreshTtl(jwtProperties.refreshTtl())
                .build();
    }

    @Bean
    public JwtEncoder appJwtEncoder() {
        var secretKey = hmacKey();
        var jwk = new OctetSequenceKey.Builder(secretKey).build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean("appJwtDecoder")
    public JwtDecoder appJwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(hmacKey()).build();
    }

    @Bean("googleJwtDecoder")
    public JwtDecoder googleJwtDecoder() {
        var decoder = NimbusJwtDecoder
                .withJwkSetUri(GOOGLE_JWKS_URI)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                new JwtClaimValidator<String>(JwtClaimNames.ISS, GOOGLE_ISSUERS::contains);

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        aud -> {
                            var audiences = aud != null ? List.copyOf(aud) : Collections.<String>emptyList();
                            return googleAuthProps.clientIds().stream().anyMatch(audiences::contains);
                        });

        var compositeValidator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), issuerValidator, audienceValidator);
        decoder.setJwtValidator(compositeValidator);

        return decoder;
    }

    private SecretKeySpec hmacKey() {
        return new SecretKeySpec(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }
}
