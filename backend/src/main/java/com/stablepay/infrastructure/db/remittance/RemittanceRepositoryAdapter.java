package com.stablepay.infrastructure.db.remittance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.model.RecentRecipient;
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
        var saved = jpaRepository.save(prepareForPersist(remittance));
        return mapper.toDomain(saved);
    }

    @Override
    public Remittance saveAndFlush(Remittance remittance) {
        var saved = jpaRepository.saveAndFlush(prepareForPersist(remittance));
        return mapper.toDomain(saved);
    }

    private RemittanceEntity prepareForPersist(Remittance remittance) {
        var entity = mapper.toEntity(remittance);
        var now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return entity;
    }

    @Override
    public Optional<Remittance> findByRemittanceId(UUID remittanceId) {
        return jpaRepository.findByRemittanceId(remittanceId).map(mapper::toDomain);
    }

    @Override
    public Page<Remittance> findBySenderId(UUID senderId, Pageable pageable) {
        return jpaRepository.findBySenderId(senderId, pageable).map(mapper::toDomain);
    }

    @Override
    public List<RecentRecipient> findRecentRecipients(UUID senderId, int limit) {
        return jpaRepository.findRecentRecipients(senderId, limit).stream()
                .map(p -> RecentRecipient.builder()
                        .name(p.getName())
                        .phone(p.getPhone())
                        .lastSentAt(p.getLastSentAt())
                        .build())
                .toList();
    }
}
