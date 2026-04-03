package com.stablepay.domain.port.inbound;

import java.util.Optional;

import com.stablepay.domain.model.Remittance;

public interface ClaimService {
    Optional<Remittance> findByClaimToken(String token);
    Remittance claim(String token, String upiId);
}
