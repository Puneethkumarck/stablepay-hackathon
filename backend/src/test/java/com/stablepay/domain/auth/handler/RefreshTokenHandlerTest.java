package com.stablepay.domain.auth.handler;

import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_RAW_REFRESH_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_REFRESH_TOKEN_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_TOKEN_HASH;
import static com.stablepay.testutil.AuthFixtures.refreshTokenBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.auth.exception.InvalidRefreshTokenException;
import com.stablepay.domain.auth.exception.RefreshTokenExpiredException;
import com.stablepay.domain.auth.model.AuthSession;
import com.stablepay.domain.auth.model.AuthTokenConfig;
import com.stablepay.domain.auth.port.AuthTokenIssuer;
import com.stablepay.domain.auth.port.RefreshTokenRepository;
import com.stablepay.domain.auth.port.TokenHasher;

@ExtendWith(MockitoExtension.class)
class RefreshTokenHandlerTest {

    private static final Instant NOW = Instant.parse("2026-04-03T10:00:00Z");
    private static final String SOME_NEW_RAW_REFRESH_TOKEN = "r1_bmV3LXJlZnJlc2gtdG9rZW4";
    private static final String SOME_NEW_TOKEN_HASH = "sha256-new-token-hash";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    private final AuthTokenConfig authTokenConfig = AuthTokenConfig.builder()
            .accessTtl(Duration.ofMinutes(15))
            .refreshTtl(Duration.ofDays(30))
            .build();

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private RefreshTokenHandler refreshTokenHandler;

    @BeforeEach
    void setUp() {
        refreshTokenHandler = new RefreshTokenHandler(
                refreshTokenRepository,
                tokenHasher,
                authTokenIssuer,
                authTokenConfig,
                clock);
    }

    @Test
    void shouldRotateRefreshToken() {
        // given
        given(tokenHasher.hash(SOME_RAW_REFRESH_TOKEN)).willReturn(SOME_TOKEN_HASH);

        var existingToken = refreshTokenBuilder().build();
        given(refreshTokenRepository.findByHash(SOME_TOKEN_HASH))
                .willReturn(Optional.of(existingToken));

        var revokedToken = existingToken.toBuilder().revokedAt(NOW).build();
        given(refreshTokenRepository.save(revokedToken)).willReturn(revokedToken);

        var newSession = AuthSession.builder()
                .accessToken("new-access-token")
                .refreshToken(SOME_NEW_RAW_REFRESH_TOKEN)
                .accessExpiresAt(NOW.plus(Duration.ofMinutes(15)))
                .build();
        given(authTokenIssuer.issue(SOME_AUTH_USER_ID)).willReturn(newSession);
        given(tokenHasher.hash(SOME_NEW_RAW_REFRESH_TOKEN)).willReturn(SOME_NEW_TOKEN_HASH);

        // when
        var result = refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(newSession);
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        // given
        given(tokenHasher.hash(SOME_RAW_REFRESH_TOKEN)).willReturn(SOME_TOKEN_HASH);
        given(refreshTokenRepository.findByHash(SOME_TOKEN_HASH)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldThrowWhenTokenRevoked() {
        // given
        given(tokenHasher.hash(SOME_RAW_REFRESH_TOKEN)).willReturn(SOME_TOKEN_HASH);

        var revokedToken = refreshTokenBuilder()
                .revokedAt(NOW.minus(Duration.ofHours(1)))
                .build();
        given(refreshTokenRepository.findByHash(SOME_TOKEN_HASH))
                .willReturn(Optional.of(revokedToken));

        // when / then
        assertThatThrownBy(() -> refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void shouldThrowWhenTokenExpired() {
        // given
        given(tokenHasher.hash(SOME_RAW_REFRESH_TOKEN)).willReturn(SOME_TOKEN_HASH);

        var expiredToken = refreshTokenBuilder()
                .expiresAt(NOW.minus(Duration.ofHours(1)))
                .build();
        given(refreshTokenRepository.findByHash(SOME_TOKEN_HASH))
                .willReturn(Optional.of(expiredToken));

        // when / then
        assertThatThrownBy(() -> refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenExpiredException.class)
                .hasMessageContaining(SOME_REFRESH_TOKEN_ID.toString());
    }
}
