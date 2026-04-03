package com.stablepay.infrastructure.db.wallet;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;
    private final WalletEntityMapper mapper;

    @Override
    public Wallet save(Wallet wallet) {
        var entity = mapper.toEntity(wallet);
        var now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Wallet> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }
}
