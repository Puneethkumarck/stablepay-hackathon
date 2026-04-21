package com.stablepay.domain.auth.port;

import java.util.Optional;
import java.util.UUID;

import com.stablepay.domain.auth.model.RefreshToken;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByHash(String tokenHash);
    RefreshToken save(RefreshToken token);
    void revokeByUserId(UUID userId);
}
