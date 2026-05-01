package com.stablepay.infrastructure.db.wallet;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;
    private final WalletEntityMapper mapper;
    private final KeyShareEncryptor keyShareEncryptor;

    @Override
    public Wallet save(Wallet wallet) {
        var entity = mapper.toEntity(wallet);
        var now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        if (wallet.id() != null) {
            jpaRepository.findById(wallet.id())
                    .filter(existing -> existing.getKeyShareDek() != null)
                    .ifPresent(existing -> {
                        entity.setKeyShareData(existing.getKeyShareData());
                        entity.setPeerKeyShareData(existing.getPeerKeyShareData());
                        entity.setKeyShareDek(existing.getKeyShareDek());
                        entity.setKeyShareIv(existing.getKeyShareIv());
                        entity.setPeerKeyShareIv(existing.getPeerKeyShareIv());
                    });
        }

        if (entity.getKeyShareDek() == null && entity.getKeyShareData() != null) {
            var encrypted = keyShareEncryptor.encrypt(
                    entity.getKeyShareData(), entity.getPeerKeyShareData());
            entity.setKeyShareData(encrypted.encryptedKeyShareData());
            entity.setPeerKeyShareData(encrypted.encryptedPeerKeyShareData());
            entity.setKeyShareDek(encrypted.encryptedDek());
            entity.setKeyShareIv(encrypted.keyShareIv());
            entity.setPeerKeyShareIv(encrypted.peerKeyShareIv());
        }

        var saved = jpaRepository.save(entity);
        return decryptAndMap(saved);
    }

    @Override
    public Optional<Wallet> findById(Long id) {
        return jpaRepository.findById(id).map(this::decryptAndMap);
    }

    @Override
    public Optional<Wallet> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id).map(this::decryptAndMap);
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(this::decryptAndMap);
    }

    @Override
    public Optional<Wallet> findByUserIdForUpdate(UUID userId) {
        return jpaRepository.findByUserIdForUpdate(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findBySolanaAddress(String solanaAddress) {
        return jpaRepository.findBySolanaAddress(solanaAddress).map(this::decryptAndMap);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    private Wallet decryptAndMap(WalletEntity entity) {
        var wallet = mapper.toDomain(entity);
        if (entity.getKeyShareDek() != null) {
            var decrypted = keyShareEncryptor.decrypt(
                    entity.getKeyShareData(), entity.getPeerKeyShareData(),
                    entity.getKeyShareDek(), entity.getKeyShareIv(), entity.getPeerKeyShareIv());
            return wallet.toBuilder()
                    .keyShareData(decrypted.keyShareData())
                    .peerKeyShareData(decrypted.peerKeyShareData())
                    .build();
        }
        return wallet;
    }
}
