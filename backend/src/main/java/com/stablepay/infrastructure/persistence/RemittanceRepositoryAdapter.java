package com.stablepay.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.model.Remittance;
import com.stablepay.domain.port.outbound.RemittanceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RemittanceRepositoryAdapter implements RemittanceRepository {

    private final RemittanceJpaRepository jpaRepository;
    private final RemittanceEntityMapper mapper;

    @Override
    public Remittance save(Remittance remittance) {
        var entity = mapper.toEntity(remittance);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Remittance> findByRemittanceId(UUID remittanceId) {
        return jpaRepository.findByRemittanceId(remittanceId).map(mapper::toDomain);
    }

    @Override
    public List<Remittance> findBySenderId(String senderId) {
        return jpaRepository.findBySenderId(senderId).stream().map(mapper::toDomain).toList();
    }
}
