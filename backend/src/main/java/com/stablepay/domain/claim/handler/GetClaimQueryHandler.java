package com.stablepay.domain.claim.handler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.claim.model.ClaimDetails;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.port.RemittanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GetClaimQueryHandler {

    private final ClaimTokenRepository claimTokenRepository;
    private final RemittanceRepository remittanceRepository;
    private final UserRepository userRepository;

    public ClaimDetails handle(String token) {
        var claimToken = claimTokenRepository.findByToken(token)
                .orElseThrow(() -> ClaimTokenNotFoundException.byToken(token));

        var remittance = remittanceRepository.findByRemittanceId(claimToken.remittanceId())
                .orElseThrow(() -> RemittanceNotFoundException.byId(claimToken.remittanceId()));

        var senderDisplayName = userRepository.findById(remittance.senderId())
                .map(user -> user.email().split("@")[0])
                .orElse("Unknown");

        return ClaimDetails.builder()
                .claimToken(claimToken)
                .remittance(remittance)
                .senderDisplayName(senderDisplayName)
                .build();
    }
}
