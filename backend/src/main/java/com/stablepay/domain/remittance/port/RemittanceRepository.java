package com.stablepay.domain.remittance.port;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.stablepay.domain.remittance.model.Remittance;

public interface RemittanceRepository {
    Remittance save(Remittance remittance);
    Optional<Remittance> findByRemittanceId(UUID remittanceId);
    Page<Remittance> findBySenderId(String senderId, Pageable pageable);
}
