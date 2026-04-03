package com.stablepay.domain.port.outbound;

import java.util.Optional;

import com.stablepay.domain.model.ClaimToken;

public interface ClaimTokenRepository {
    ClaimToken save(ClaimToken claimToken);
    Optional<ClaimToken> findByToken(String token);
}
