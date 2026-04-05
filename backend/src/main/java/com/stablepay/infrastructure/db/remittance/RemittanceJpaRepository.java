package com.stablepay.infrastructure.db.remittance;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface RemittanceJpaRepository extends JpaRepository<RemittanceEntity, Long> {
    Optional<RemittanceEntity> findByRemittanceId(UUID remittanceId);
    Page<RemittanceEntity> findBySenderId(String senderId, Pageable pageable);
}
