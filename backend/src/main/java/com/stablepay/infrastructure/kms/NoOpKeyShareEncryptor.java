package com.stablepay.infrastructure.kms;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stablepay.kms.enabled", havingValue = "false")
public class NoOpKeyShareEncryptor implements KeyShareEncryptor {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void validate() {
        log.warn("KMS disabled — key shares stored unencrypted");
        var count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM wallets WHERE key_share_dek IS NOT NULL", Long.class);
        if (count != null && count > 0) {
            throw new IllegalStateException(
                    "KMS is disabled but " + count + " wallet(s) have encrypted key shares. "
                    + "Run the reverse backfill (stablepay.kms.reverse-backfill-key-shares=true) "
                    + "before disabling KMS.");
        }
    }

    @Override
    public EncryptedKeyMaterial encrypt(byte[] keyShareData, byte[] peerKeyShareData) {
        return EncryptedKeyMaterial.builder()
                .encryptedKeyShareData(keyShareData)
                .encryptedPeerKeyShareData(peerKeyShareData)
                .encryptedDek(null)
                .keyShareIv(null)
                .peerKeyShareIv(null)
                .build();
    }

    @Override
    public DecryptedKeyMaterial decrypt(byte[] encKeyShare, byte[] encPeerKeyShare,
                                         byte[] encDek, byte[] iv, byte[] peerIv) {
        return DecryptedKeyMaterial.builder()
                .keyShareData(encKeyShare)
                .peerKeyShareData(encPeerKeyShare)
                .build();
    }
}
