package com.stablepay.infrastructure.db.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;

@ExtendWith(MockitoExtension.class)
class KeyShareBackfillRunnerTest {

    private static final Long WALLET_ID_1 = 1L;
    private static final Long WALLET_ID_2 = 2L;
    private static final byte[] KEY_SHARE = {10, 20, 30};
    private static final byte[] PEER_KEY_SHARE = {40, 50, 60};
    private static final byte[] ENCRYPTED_KEY_SHARE = {101, 102, 103};
    private static final byte[] ENCRYPTED_PEER_KEY_SHARE = {104, 105, 106};
    private static final byte[] ENCRYPTED_DEK = {21, 22, 23};
    private static final byte[] KEY_SHARE_IV = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final byte[] PEER_KEY_SHARE_IV = {13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24};

    @Mock
    private WalletJpaRepository walletJpaRepository;

    @Mock
    private KeyShareEncryptor keyShareEncryptor;

    @Captor
    private ArgumentCaptor<WalletEntity> entityCaptor;

    @InjectMocks
    private KeyShareBackfillRunner runner;

    @Test
    void shouldEncryptKeySharesForWalletsWithNullDek() {
        // given
        var entity = WalletEntity.builder()
                .id(WALLET_ID_1)
                .keyShareData(KEY_SHARE)
                .peerKeyShareData(PEER_KEY_SHARE)
                .build();
        var encryptedMaterial = EncryptedKeyMaterial.builder()
                .encryptedKeyShareData(ENCRYPTED_KEY_SHARE)
                .encryptedPeerKeyShareData(ENCRYPTED_PEER_KEY_SHARE)
                .encryptedDek(ENCRYPTED_DEK)
                .keyShareIv(KEY_SHARE_IV)
                .peerKeyShareIv(PEER_KEY_SHARE_IV)
                .build();
        given(walletJpaRepository.findById(WALLET_ID_1)).willReturn(Optional.of(entity));
        given(keyShareEncryptor.encrypt(KEY_SHARE, PEER_KEY_SHARE)).willReturn(encryptedMaterial);
        given(walletJpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        runner.migrateWallet(WALLET_ID_1);

        // then
        var saved = entityCaptor.getValue();
        assertThat(saved.getKeyShareData()).isEqualTo(ENCRYPTED_KEY_SHARE);
        assertThat(saved.getPeerKeyShareData()).isEqualTo(ENCRYPTED_PEER_KEY_SHARE);
        assertThat(saved.getKeyShareDek()).isEqualTo(ENCRYPTED_DEK);
        assertThat(saved.getKeyShareIv()).isEqualTo(KEY_SHARE_IV);
        assertThat(saved.getPeerKeyShareIv()).isEqualTo(PEER_KEY_SHARE_IV);
    }

    @Test
    void shouldContinueOnFailureAndMigrateRemainingWallets() {
        // given
        var entity2 = WalletEntity.builder()
                .id(WALLET_ID_2)
                .keyShareData(KEY_SHARE)
                .peerKeyShareData(PEER_KEY_SHARE)
                .build();
        var encryptedMaterial = EncryptedKeyMaterial.builder()
                .encryptedKeyShareData(ENCRYPTED_KEY_SHARE)
                .encryptedPeerKeyShareData(ENCRYPTED_PEER_KEY_SHARE)
                .encryptedDek(ENCRYPTED_DEK)
                .keyShareIv(KEY_SHARE_IV)
                .peerKeyShareIv(PEER_KEY_SHARE_IV)
                .build();
        given(walletJpaRepository.findIdsWithNullDek()).willReturn(List.of(WALLET_ID_1, WALLET_ID_2));
        given(walletJpaRepository.findById(WALLET_ID_1)).willReturn(Optional.empty());
        given(walletJpaRepository.findById(WALLET_ID_2)).willReturn(Optional.of(entity2));
        given(keyShareEncryptor.encrypt(KEY_SHARE, PEER_KEY_SHARE)).willReturn(encryptedMaterial);
        given(walletJpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        runner.run(null);

        // then
        then(walletJpaRepository).should().save(entityCaptor.getValue());
        var saved = entityCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(WALLET_ID_2);
        assertThat(saved.getKeyShareDek()).isEqualTo(ENCRYPTED_DEK);
    }

    @Test
    void shouldZeroPlaintextAfterEncryption() {
        // given
        byte[] keyShare = {10, 20, 30};
        byte[] peerKeyShare = {40, 50, 60};
        var entity = WalletEntity.builder()
                .id(WALLET_ID_1)
                .keyShareData(keyShare)
                .peerKeyShareData(peerKeyShare)
                .build();
        var encryptedMaterial = EncryptedKeyMaterial.builder()
                .encryptedKeyShareData(ENCRYPTED_KEY_SHARE)
                .encryptedPeerKeyShareData(ENCRYPTED_PEER_KEY_SHARE)
                .encryptedDek(ENCRYPTED_DEK)
                .keyShareIv(KEY_SHARE_IV)
                .peerKeyShareIv(PEER_KEY_SHARE_IV)
                .build();
        given(walletJpaRepository.findById(WALLET_ID_1)).willReturn(Optional.of(entity));
        given(keyShareEncryptor.encrypt(keyShare, peerKeyShare)).willReturn(encryptedMaterial);
        given(walletJpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        runner.migrateWallet(WALLET_ID_1);

        // then
        assertThat(keyShare).containsOnly(0);
        assertThat(peerKeyShare).containsOnly(0);
    }
}
