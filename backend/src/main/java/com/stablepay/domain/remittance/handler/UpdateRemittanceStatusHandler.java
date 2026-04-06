package com.stablepay.domain.remittance.handler;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class UpdateRemittanceStatusHandler {

    private final RemittanceRepository remittanceRepository;

    public void handle(UUID remittanceId, RemittanceStatus targetStatus) {
        var remittance = remittanceRepository.findByRemittanceId(remittanceId)
                .orElseThrow(() -> RemittanceNotFoundException.byId(remittanceId));

        if (!remittance.status().canTransitionTo(targetStatus)) {
            throw InvalidRemittanceStateException.forTransition(remittance.status(), targetStatus);
        }

        var updated = remittance.toBuilder().status(targetStatus).build();
        remittanceRepository.save(updated);
        log.info("Remittance {} status updated: {} → {}", remittanceId, remittance.status(), targetStatus);
    }
}
