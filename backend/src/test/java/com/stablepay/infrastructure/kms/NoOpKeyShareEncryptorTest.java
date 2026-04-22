package com.stablepay.infrastructure.kms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;

class NoOpKeyShareEncryptorTest {

    private static final byte[] KEY_SHARE = {1, 2, 3, 4, 5};
    private static final byte[] PEER_KEY_SHARE = {6, 7, 8, 9, 10};

    private final NoOpKeyShareEncryptor encryptor = new NoOpKeyShareEncryptor();

    @Test
    void shouldReturnKeySharesUnchangedOnEncrypt() {
        // when
        var result = encryptor.encrypt(KEY_SHARE, PEER_KEY_SHARE);

        // then
        var expected = EncryptedKeyMaterial.builder()
                .encryptedKeyShareData(KEY_SHARE)
                .encryptedPeerKeyShareData(PEER_KEY_SHARE)
                .encryptedDek(null)
                .keyShareIv(null)
                .peerKeyShareIv(null)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnKeySharesUnchangedOnDecrypt() {
        // when
        var result = encryptor.decrypt(KEY_SHARE, PEER_KEY_SHARE, null, null, null);

        // then
        var expected = DecryptedKeyMaterial.builder()
                .keyShareData(KEY_SHARE)
                .peerKeyShareData(PEER_KEY_SHARE)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
