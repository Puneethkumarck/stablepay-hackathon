package com.stablepay.domain.remittance.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stablepay.domain.remittance.model.Remittance;

public interface RemittanceRepository {
    Remittance save(Remittance remittance);
    Optional<Remittance> findByRemittanceId(UUID remittanceId);
    List<Remittance> findBySenderId(String senderId);
}
