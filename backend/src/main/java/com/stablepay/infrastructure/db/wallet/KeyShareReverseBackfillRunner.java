package com.stablepay.infrastructure.db.wallet;

import java.util.Arrays;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.stablepay.domain.wallet.port.KeyShareEncryptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stablepay.kms.reverse-backfill-key-shares", havingValue = "true")
class KeyShareReverseBackfillRunner implements ApplicationRunner {

    private final WalletJpaRepository walletJpaRepository;
    private final KeyShareEncryptor keyShareEncryptor;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        var walletIds = walletJpaRepository.findIdsWithNonNullDek();
        log.info("Starting reverse backfill for {} wallets", walletIds.size());

        int success = 0;
        for (int i = 0; i < walletIds.size(); i++) {
            try {
                reverseWallet(walletIds.get(i));
                success++;
                log.info("Reversed wallet {} ({}/{})", walletIds.get(i), i + 1, walletIds.size());
            } catch (Exception e) {
                log.error("Failed to reverse wallet {}: {}", walletIds.get(i), e.getMessage(), e);
            }
        }

        log.info("Reverse backfill complete: {}/{} wallets reversed", success, walletIds.size());
    }

    void reverseWallet(Long walletId) {
        transactionTemplate.executeWithoutResult(status -> {
            var entity = walletJpaRepository.findById(walletId).orElseThrow();
            if (entity.getKeyShareDek() == null) {
                log.info("Skipping wallet {} — already decrypted", walletId);
                return;
            }

            var decrypted = keyShareEncryptor.decrypt(
                    entity.getKeyShareData(), entity.getPeerKeyShareData(),
                    entity.getKeyShareDek(), entity.getKeyShareIv(), entity.getPeerKeyShareIv());

            byte[] keyShareData = decrypted.keyShareData();
            byte[] peerKeyShareData = decrypted.peerKeyShareData();

            try {
                entity.setKeyShareData(keyShareData.clone());
                entity.setPeerKeyShareData(peerKeyShareData.clone());
                entity.setKeyShareDek(null);
                entity.setKeyShareIv(null);
                entity.setPeerKeyShareIv(null);
                walletJpaRepository.save(entity);
            } finally {
                Arrays.fill(keyShareData, (byte) 0);
                Arrays.fill(peerKeyShareData, (byte) 0);
            }
        });
    }
}
