package com.stablepay.infrastructure.db.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.port.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public Optional<AppUser> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public AppUser save(AppUser user) {
        var entity = mapper.toEntity(user);
        var now = Instant.now();
        var createdAt = Optional.ofNullable(entity.getCreatedAt())
                .or(() -> jpaRepository.findById(user.id()).map(UserEntity::getCreatedAt))
                .orElse(now);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(now);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
