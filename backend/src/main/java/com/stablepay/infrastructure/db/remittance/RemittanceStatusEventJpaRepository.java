package com.stablepay.infrastructure.db.remittance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RemittanceStatusEventJpaRepository extends JpaRepository<RemittanceStatusEventEntity, Long> {
    List<RemittanceStatusEventEntity> findByRemittanceIdOrderByCreatedAtAscIdAsc(UUID remittanceId);
}
