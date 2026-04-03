package com.stablepay.domain.port.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stablepay.domain.model.Remittance;

public interface RemittanceService {
    Remittance create(String senderId, String recipientPhone, BigDecimal amountUsdc);
    Optional<Remittance> findByRemittanceId(UUID remittanceId);
    List<Remittance> findBySenderId(String senderId);
}
