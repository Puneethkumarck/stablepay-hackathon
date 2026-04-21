package com.stablepay.domain.auth.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.exception.InvalidRefreshTokenException;
import com.stablepay.domain.auth.exception.RefreshTokenExpiredException;
import com.stablepay.domain.auth.model.AuthSession;
import com.stablepay.domain.auth.model.AuthTokenConfig;
import com.stablepay.domain.auth.model.RefreshToken;
import com.stablepay.domain.auth.port.AuthTokenIssuer;
import com.stablepay.domain.auth.port.RefreshTokenRepository;
import com.stablepay.domain.auth.port.TokenHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final AuthTokenIssuer authTokenIssuer;
    private final AuthTokenConfig authTokenConfig;
    private final Clock clock;

    public AuthSession handle(String rawRefreshToken, String ip, String userAgent) {
        var now = Instant.now(clock);
        var tokenHash = tokenHasher.hash(rawRefreshToken);

        var token = refreshTokenRepository.findByHashForUpdate(tokenHash)
                .orElseThrow(() -> InvalidRefreshTokenException.of("not found"));

        if (token.revokedAt() != null) {
            throw InvalidRefreshTokenException.of("revoked");
        }

        if (!now.isBefore(token.expiresAt())) {
            throw RefreshTokenExpiredException.forToken(token.id());
        }

        refreshTokenRepository.save(token.toBuilder().revokedAt(now).build());

        var newSession = authTokenIssuer.issue(token.userId());
        var newHash = tokenHasher.hash(newSession.refreshToken());

        var newRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(token.userId())
                .tokenHash(newHash)
                .expiresAt(now.plus(authTokenConfig.refreshTtl()))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("REFRESH userId={} ip={} userAgent={}", token.userId(), ip, userAgent);

        return newSession;
    }
}
