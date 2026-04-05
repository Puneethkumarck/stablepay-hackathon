package com.stablepay.domain.claim.handler;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.claim.exception.ClaimAlreadyClaimedException;
import com.stablepay.domain.claim.exception.ClaimTokenExpiredException;
import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.claim.model.ClaimDetails;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubmitClaimHandler {

    private final ClaimTokenRepository claimTokenRepository;
    private final RemittanceRepository remittanceRepository;

    public ClaimDetails handle(String token, String upiId) {
        var claimToken = claimTokenRepository.findByToken(token)
                .orElseThrow(() -> ClaimTokenNotFoundException.byToken(token));

        if (claimToken.claimed()) {
            throw ClaimAlreadyClaimedException.forToken(token);
        }

        if (claimToken.expiresAt() != null && claimToken.expiresAt().isBefore(Instant.now())) {
            throw ClaimTokenExpiredException.forToken(token);
        }

        var remittance = remittanceRepository.findByRemittanceId(claimToken.remittanceId())
                .orElseThrow(() -> RemittanceNotFoundException.byId(claimToken.remittanceId()));

        if (remittance.status() != RemittanceStatus.ESCROWED) {
            throw InvalidRemittanceStateException.forClaim(remittance.status());
        }

        var updatedClaim = claimToken.toBuilder()
                .claimed(true)
                .upiId(upiId)
                .build();
        var savedClaim = claimTokenRepository.save(updatedClaim);

        var updatedRemittance = remittance.toBuilder()
                .status(RemittanceStatus.CLAIMED)
                .build();
        var savedRemittance = remittanceRepository.save(updatedRemittance);

        log.info("Claim submitted for token={}, remittanceId={}, upiId={}",
                token, claimToken.remittanceId(), upiId);

        return ClaimDetails.builder()
                .claimToken(savedClaim)
                .remittance(savedRemittance)
                .build();
    }
}
