package com.stablepay.infrastructure.db.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.auth.model.SocialIdentity;
import com.stablepay.domain.auth.port.SocialIdentityRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SocialIdentityRepositoryAdapter implements SocialIdentityRepository {

    private final SocialIdentityJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public Optional<SocialIdentity> findByProviderAndSubject(String provider, String subject) {
        return jpaRepository.findByProviderAndSubject(provider, subject).map(mapper::toDomain);
    }

    @Override
    public SocialIdentity save(SocialIdentity identity, UUID userId) {
        var entity = mapper.toEntity(identity);
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setCreatedAt(Instant.now());
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
