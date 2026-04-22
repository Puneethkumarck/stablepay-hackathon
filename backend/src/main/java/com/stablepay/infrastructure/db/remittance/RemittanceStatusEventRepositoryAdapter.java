package com.stablepay.infrastructure.db.remittance;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.model.RemittanceStatusEvent;
import com.stablepay.domain.remittance.port.RemittanceStatusEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RemittanceStatusEventRepositoryAdapter implements RemittanceStatusEventRepository {

    private final RemittanceStatusEventJpaRepository jpaRepository;
    private final RemittanceStatusEventEntityMapper mapper;

    @Override
    public RemittanceStatusEvent save(RemittanceStatusEvent event) {
        var entity = mapper.toEntity(event);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<RemittanceStatusEvent> findByRemittanceId(UUID remittanceId) {
        return jpaRepository.findByRemittanceIdOrderByCreatedAtAscIdAsc(remittanceId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
