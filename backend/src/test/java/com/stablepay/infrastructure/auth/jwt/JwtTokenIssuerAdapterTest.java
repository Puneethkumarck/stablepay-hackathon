package com.stablepay.infrastructure.auth.jwt;

import static com.stablepay.testutil.AuthFixtures.SOME_ACCESS_TTL;
import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_JWT_SECRET;
import static com.stablepay.testutil.AuthFixtures.SOME_REFRESH_TTL;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.stablepay.domain.auth.model.AuthSession;
import com.stablepay.domain.auth.model.AuthTokenConfig;

class JwtTokenIssuerAdapterTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-20T12:00:00Z");

    private JwtTokenIssuerAdapter adapter;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() {
        var secretKey = new SecretKeySpec(
                SOME_JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var jwk = new OctetSequenceKey.Builder(secretKey).build();
        var jwkSource = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk));
        var encoder = new NimbusJwtEncoder(jwkSource);

        var tokenConfig = AuthTokenConfig.builder()
                .accessTtl(SOME_ACCESS_TTL)
                .refreshTtl(SOME_REFRESH_TTL)
                .build();
        var fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        var refreshTokenGenerator = new RefreshTokenGenerator();
        adapter = new JwtTokenIssuerAdapter(encoder, tokenConfig, fixedClock, refreshTokenGenerator);

        decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        var timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(fixedClock);
        decoder.setJwtValidator(timestampValidator);
    }

    @Test
    void shouldIssueValidAuthSessionWithCorrectClaimsAndExpiry() {
        // given
        var expectedExpiresAt = FIXED_NOW.plus(SOME_ACCESS_TTL);

        // when
        var result = adapter.issue(SOME_AUTH_USER_ID);

        // then
        var decoded = decoder.decode(result.accessToken());
        var expected = AuthSession.builder()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .accessExpiresAt(expectedExpiresAt)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        assertThat(decoded.getSubject()).isEqualTo(SOME_AUTH_USER_ID.toString());
        assertThat(decoded.getIssuedAt()).isEqualTo(FIXED_NOW);
        assertThat(decoded.getExpiresAt()).isEqualTo(expectedExpiresAt);
    }

    @Test
    void shouldGenerateRefreshTokenWithR1Prefix() {
        // given

        // when
        var result = adapter.issue(SOME_AUTH_USER_ID);

        // then
        assertThat(result.refreshToken()).matches("r1_.*");
    }
}
