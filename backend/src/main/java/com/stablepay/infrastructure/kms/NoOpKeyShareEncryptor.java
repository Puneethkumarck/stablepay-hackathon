package com.stablepay.infrastructure.kms;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "stablepay.kms.enabled", havingValue = "false")
public class NoOpKeyShareEncryptor implements KeyShareEncryptor {

    @PostConstruct
    void warnDisabled() {
        log.warn("KMS disabled — key shares stored unencrypted");
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
