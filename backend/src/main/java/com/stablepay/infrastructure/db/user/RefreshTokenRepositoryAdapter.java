package com.stablepay.infrastructure.db.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.auth.model.RefreshToken;
import com.stablepay.domain.auth.port.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        var entity = mapper.toEntity(token);
        entity.setIssuedAt(Instant.now());
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void revokeByUserId(UUID userId) {
        jpaRepository.revokeByUserId(userId);
    }
}
