package com.stablepay.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RemittanceJpaRepository extends JpaRepository<RemittanceEntity, Long> {
    Optional<RemittanceEntity> findByRemittanceId(UUID remittanceId);
    List<RemittanceEntity> findBySenderId(String senderId);
}
