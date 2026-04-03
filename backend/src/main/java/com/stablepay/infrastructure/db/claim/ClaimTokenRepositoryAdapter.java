package com.stablepay.infrastructure.db.claim;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.stablepay.domain.claim.model.ClaimToken;
import com.stablepay.domain.claim.port.ClaimTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ClaimTokenRepositoryAdapter implements ClaimTokenRepository {

    private final ClaimTokenJpaRepository jpaRepository;
    private final ClaimTokenEntityMapper mapper;

    @Override
    public ClaimToken save(ClaimToken claimToken) {
        var entity = mapper.toEntity(claimToken);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ClaimToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }
}
