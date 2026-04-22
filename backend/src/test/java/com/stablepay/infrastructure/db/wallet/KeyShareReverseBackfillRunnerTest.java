package com.stablepay.infrastructure.db.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;

@ExtendWith(MockitoExtension.class)
class KeyShareReverseBackfillRunnerTest {

    private static final Long WALLET_ID_1 = 1L;
    private static final byte[] ENCRYPTED_KEY_SHARE = {101, 102, 103};
    private static final byte[] ENCRYPTED_PEER_KEY_SHARE = {104, 105, 106};
    private static final byte[] ENCRYPTED_DEK = {21, 22, 23};
    private static final byte[] KEY_SHARE_IV = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final byte[] PEER_KEY_SHARE_IV = {13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24};
    private static final byte[] PLAINTEXT_KEY_SHARE = {10, 20, 30};
    private static final byte[] PLAINTEXT_PEER_KEY_SHARE = {40, 50, 60};

    @Mock
    private WalletJpaRepository walletJpaRepository;

    @Mock
    private KeyShareEncryptor keyShareEncryptor;

    @Captor
    private ArgumentCaptor<WalletEntity> entityCaptor;

    @InjectMocks
    private KeyShareReverseBackfillRunner runner;

    @Test
    void shouldDecryptAndNullEncryptionColumnsForWalletsWithNonNullDek() {
        // given
        var entity = WalletEntity.builder()
                .id(WALLET_ID_1)
                .keyShareData(ENCRYPTED_KEY_SHARE)
                .peerKeyShareData(ENCRYPTED_PEER_KEY_SHARE)
                .keyShareDek(ENCRYPTED_DEK)
                .keyShareIv(KEY_SHARE_IV)
                .peerKeyShareIv(PEER_KEY_SHARE_IV)
                .build();
        var decryptedMaterial = DecryptedKeyMaterial.builder()
                .keyShareData(PLAINTEXT_KEY_SHARE)
                .peerKeyShareData(PLAINTEXT_PEER_KEY_SHARE)
                .build();
        given(walletJpaRepository.findById(WALLET_ID_1)).willReturn(Optional.of(entity));
        given(keyShareEncryptor.decrypt(
                ENCRYPTED_KEY_SHARE, ENCRYPTED_PEER_KEY_SHARE,
                ENCRYPTED_DEK, KEY_SHARE_IV, PEER_KEY_SHARE_IV))
                .willReturn(decryptedMaterial);
        given(walletJpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        runner.reverseWallet(WALLET_ID_1);

        // then
        var saved = entityCaptor.getValue();
        assertThat(saved.getKeyShareData()).isEqualTo(PLAINTEXT_KEY_SHARE);
        assertThat(saved.getPeerKeyShareData()).isEqualTo(PLAINTEXT_PEER_KEY_SHARE);
        assertThat(saved.getKeyShareDek()).isNull();
        assertThat(saved.getKeyShareIv()).isNull();
        assertThat(saved.getPeerKeyShareIv()).isNull();
    }

    @Test
    void shouldZeroPlaintextAfterReverse() {
        // given
        byte[] plaintextKey = {10, 20, 30};
        byte[] plaintextPeer = {40, 50, 60};
        var entity = WalletEntity.builder()
                .id(WALLET_ID_1)
                .keyShareData(ENCRYPTED_KEY_SHARE)
                .peerKeyShareData(ENCRYPTED_PEER_KEY_SHARE)
                .keyShareDek(ENCRYPTED_DEK)
                .keyShareIv(KEY_SHARE_IV)
                .peerKeyShareIv(PEER_KEY_SHARE_IV)
                .build();
        var decryptedMaterial = DecryptedKeyMaterial.builder()
                .keyShareData(plaintextKey)
                .peerKeyShareData(plaintextPeer)
                .build();
        given(walletJpaRepository.findById(WALLET_ID_1)).willReturn(Optional.of(entity));
        given(keyShareEncryptor.decrypt(
                ENCRYPTED_KEY_SHARE, ENCRYPTED_PEER_KEY_SHARE,
                ENCRYPTED_DEK, KEY_SHARE_IV, PEER_KEY_SHARE_IV))
                .willReturn(decryptedMaterial);
        given(walletJpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        runner.reverseWallet(WALLET_ID_1);

        // then
        assertThat(plaintextKey).containsOnly(0);
        assertThat(plaintextPeer).containsOnly(0);
    }
}
