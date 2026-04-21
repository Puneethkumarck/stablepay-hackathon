package com.stablepay.infrastructure.auth.jwt;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.stablepay.domain.auth.model.AuthSession;
import com.stablepay.domain.auth.model.AuthTokenConfig;
import com.stablepay.domain.auth.port.AuthTokenIssuer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuerAdapter implements AuthTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final AuthTokenConfig authTokenConfig;
    private final Clock clock;
    private final RefreshTokenGenerator refreshTokenGenerator;

    @Override
    public AuthSession issue(UUID userId) {
        requireNonNull(userId, "userId cannot be null");
        var now = Instant.now(clock);
        var expiresAt = now.plus(authTokenConfig.accessTtl());

        var claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        var header = JwsHeader.with(() -> JwsAlgorithms.HS256).build();
        var jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        var refreshToken = refreshTokenGenerator.generate();

        return AuthSession.builder()
                .accessToken(jwt.getTokenValue())
                .refreshToken(refreshToken)
                .accessExpiresAt(expiresAt)
                .build();
    }
}
