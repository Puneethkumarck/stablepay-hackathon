package com.stablepay.domain.remittance.handler;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.common.PiiMasking;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.port.RemittanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemittancePayoutWriter {

    private static final int FAILURE_REASON_MAX = 500;
    private static final Pattern UPI_HANDLE = Pattern.compile("\\S+@\\S+");

    private final RemittanceRepository remittanceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<DisbursementResult> findExistingPayout(UUID remittanceId) {
        return remittanceRepository.findByRemittanceId(remittanceId)
                .filter(r -> r.payoutId() != null)
                .map(r -> DisbursementResult.builder()
                        .providerId(r.payoutId())
                        .providerStatus(r.payoutProviderStatus())
                        .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writePayoutId(UUID remittanceId, String providerId, String providerStatus) {
        var remittance = remittanceRepository.findByRemittanceId(remittanceId)
                .orElseThrow(() -> RemittanceNotFoundException.byId(remittanceId));
        try {
            remittanceRepository.save(remittance.toBuilder()
                    .payoutId(providerId)
                    .payoutProviderStatus(providerStatus)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "Duplicate payout_id write for remittance " + remittanceId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeFailureReason(UUID remittanceId, String rawReason) {
        var remittance = remittanceRepository.findByRemittanceId(remittanceId)
                .orElseThrow(() -> RemittanceNotFoundException.byId(remittanceId));
        remittanceRepository.save(remittance.toBuilder()
                .payoutFailureReason(sanitize(rawReason))
                .build());
    }

    private static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        var truncated = input.length() <= FAILURE_REASON_MAX
                ? input
                : input.substring(0, FAILURE_REASON_MAX);
        return UPI_HANDLE.matcher(truncated).replaceAll(match -> PiiMasking.maskUpi(match.group()));
    }
}
