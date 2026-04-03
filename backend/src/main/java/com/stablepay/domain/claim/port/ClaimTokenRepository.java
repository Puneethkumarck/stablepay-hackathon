package com.stablepay.domain.claim.port;

import java.util.Optional;

import com.stablepay.domain.claim.model.ClaimToken;

public interface ClaimTokenRepository {
    ClaimToken save(ClaimToken claimToken);
    Optional<ClaimToken> findByToken(String token);
}
