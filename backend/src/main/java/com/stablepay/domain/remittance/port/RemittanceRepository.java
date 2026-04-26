package com.stablepay.domain.remittance.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.stablepay.domain.remittance.model.RecentRecipient;
import com.stablepay.domain.remittance.model.Remittance;

public interface RemittanceRepository {
    Remittance save(Remittance remittance);
    Remittance saveAndFlush(Remittance remittance);
    Optional<Remittance> findByRemittanceId(UUID remittanceId);
    Page<Remittance> findBySenderId(UUID senderId, Pageable pageable);
    List<RecentRecipient> findRecentRecipients(UUID senderId, int limit);
}
