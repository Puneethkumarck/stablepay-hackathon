package com.stablepay.infrastructure.db.wallet;

import java.util.Arrays;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.wallet.port.KeyShareEncryptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stablepay.kms.backfill-key-shares", havingValue = "true")
class KeyShareBackfillRunner implements ApplicationRunner {

    private final WalletJpaRepository walletJpaRepository;
    private final KeyShareEncryptor keyShareEncryptor;

    @Override
    public void run(ApplicationArguments args) {
        var walletIds = walletJpaRepository.findIdsWithNullDek();
        log.info("Starting key share backfill for {} wallets", walletIds.size());

        int success = 0;
        for (int i = 0; i < walletIds.size(); i++) {
            try {
                migrateWallet(walletIds.get(i));
                success++;
                log.info("Migrated wallet {} ({}/{})", walletIds.get(i), i + 1, walletIds.size());
            } catch (Exception e) {
                log.error("Failed to migrate wallet {}: {}", walletIds.get(i), e.getMessage(), e);
            }
        }

        log.info("Backfill complete: {}/{} wallets migrated", success, walletIds.size());
    }

    @Transactional
    void migrateWallet(Long walletId) {
        var entity = walletJpaRepository.findById(walletId).orElseThrow();
        byte[] keyShareData = entity.getKeyShareData();
        byte[] peerKeyShareData = entity.getPeerKeyShareData();

        try {
            var encrypted = keyShareEncryptor.encrypt(keyShareData, peerKeyShareData);
            entity.setKeyShareData(encrypted.encryptedKeyShareData());
            entity.setPeerKeyShareData(encrypted.encryptedPeerKeyShareData());
            entity.setKeyShareDek(encrypted.encryptedDek());
            entity.setKeyShareIv(encrypted.keyShareIv());
            entity.setPeerKeyShareIv(encrypted.peerKeyShareIv());
            walletJpaRepository.save(entity);
        } finally {
            Arrays.fill(keyShareData, (byte) 0);
            Arrays.fill(peerKeyShareData, (byte) 0);
        }
    }
}
