package com.stablepay.domain.remittance.handler;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.port.RemittanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRemittanceQueryHandler {

    private final RemittanceRepository remittanceRepository;

    public Remittance handle(UUID remittanceId, UUID authenticatedUserId) {
        var remittance = remittanceRepository.findByRemittanceId(remittanceId)
                .orElseThrow(() -> RemittanceNotFoundException.byId(remittanceId));

        if (!remittance.senderId().equals(authenticatedUserId)) {
            throw RemittanceNotFoundException.byId(remittanceId);
        }

        return remittance;
    }
}
