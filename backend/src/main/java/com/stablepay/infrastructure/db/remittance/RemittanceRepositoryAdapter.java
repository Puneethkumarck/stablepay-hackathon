package com.stablepay.infrastructure.db.remittance;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.port.RemittanceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RemittanceRepositoryAdapter implements RemittanceRepository {

    private final RemittanceJpaRepository jpaRepository;
    private final RemittanceEntityMapper mapper;

    @Override
    public Remittance save(Remittance remittance) {
        var entity = mapper.toEntity(remittance);
        var now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        var saved = jpaRepository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Remittance> findByRemittanceId(UUID remittanceId) {
        return jpaRepository.findByRemittanceId(remittanceId).map(mapper::toDomain);
    }

    @Override
    public Page<Remittance> findBySenderId(String senderId, Pageable pageable) {
        return jpaRepository.findBySenderId(senderId, pageable).map(mapper::toDomain);
    }
}
