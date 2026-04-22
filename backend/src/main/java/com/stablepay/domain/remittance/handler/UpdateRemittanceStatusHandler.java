package com.stablepay.domain.remittance.handler;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.RemittanceStatusEvent;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.domain.remittance.port.RemittanceStatusEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateRemittanceStatusHandler {

    private static final Map<RemittanceStatus, String> STATUS_MESSAGES = Map.of(
            RemittanceStatus.ESCROWED, "Funds secured on-chain",
            RemittanceStatus.CLAIMED, "Recipient claimed",
            RemittanceStatus.DELIVERED, "INR deposited to recipient's bank",
            RemittanceStatus.REFUNDED, "Refunded to sender",
            RemittanceStatus.CANCELLED, "Remittance cancelled",
            RemittanceStatus.DEPOSIT_FAILED, "Processing failed",
            RemittanceStatus.CLAIM_FAILED, "Processing failed",
            RemittanceStatus.DISBURSEMENT_FAILED, "Processing failed",
            RemittanceStatus.REFUND_FAILED, "Processing failed"
    );

    private final RemittanceRepository remittanceRepository;
    private final RemittanceStatusEventRepository remittanceStatusEventRepository;

    public void handle(UUID remittanceId, RemittanceStatus targetStatus) {
        var remittance = remittanceRepository.findByRemittanceId(remittanceId)
                .orElseThrow(() -> RemittanceNotFoundException.byId(remittanceId));

        if (!remittance.status().canTransitionTo(targetStatus)) {
            throw InvalidRemittanceStateException.forTransition(remittance.status(), targetStatus);
        }

        var updated = remittance.toBuilder().status(targetStatus).build();
        remittanceRepository.save(updated);

        remittanceStatusEventRepository.save(RemittanceStatusEvent.builder()
                .remittanceId(remittanceId)
                .status(targetStatus)
                .message(STATUS_MESSAGES.getOrDefault(targetStatus, "Status updated"))
                .createdAt(Instant.now())
                .build());

        log.info("Remittance {} status updated: {} → {}", remittanceId, remittance.status(), targetStatus);
    }
}
