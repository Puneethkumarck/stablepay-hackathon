package com.stablepay.infrastructure.db.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

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

    private KeyShareReverseBackfillRunner runner;

    @BeforeEach
    void setUp() {
        var transactionTemplate = new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {}

            @Override
            public void rollback(TransactionStatus status) {}
        });
        runner = new KeyShareReverseBackfillRunner(walletJpaRepository, keyShareEncryptor, transactionTemplate);
    }

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
        var savedKeyShareData = new byte[1][];
        var savedPeerKeyShareData = new byte[1][];
        given(walletJpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> {
                    var saved = invocation.getArgument(0, WalletEntity.class);
                    savedKeyShareData[0] = saved.getKeyShareData().clone();
                    savedPeerKeyShareData[0] = saved.getPeerKeyShareData().clone();
                    return saved;
                });

        // when
        runner.reverseWallet(WALLET_ID_1);

        // then
        assertThat(savedKeyShareData[0]).isEqualTo(PLAINTEXT_KEY_SHARE);
        assertThat(savedPeerKeyShareData[0]).isEqualTo(PLAINTEXT_PEER_KEY_SHARE);
        var saved = entityCaptor.getValue();
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
        var keyShareRef = new byte[1][];
        var peerKeyShareRef = new byte[1][];
        given(walletJpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> {
                    var saved = invocation.getArgument(0, WalletEntity.class);
                    keyShareRef[0] = saved.getKeyShareData();
                    peerKeyShareRef[0] = saved.getPeerKeyShareData();
                    return saved;
                });

        // when
        runner.reverseWallet(WALLET_ID_1);

        // then
        assertThat(keyShareRef[0]).containsOnly(0);
        assertThat(peerKeyShareRef[0]).containsOnly(0);
    }
}
