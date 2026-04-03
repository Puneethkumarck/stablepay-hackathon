package com.stablepay.domain.port.outbound;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stablepay.domain.model.Remittance;

public interface RemittanceRepository {
    Remittance save(Remittance remittance);
    Optional<Remittance> findByRemittanceId(UUID remittanceId);
    List<Remittance> findBySenderId(String senderId);
}
