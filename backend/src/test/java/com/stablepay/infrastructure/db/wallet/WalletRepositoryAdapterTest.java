package com.stablepay.infrastructure.db.wallet;

import static com.stablepay.testutil.WalletFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PEER_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.SOME_WALLET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;
import com.stablepay.testutil.WalletFixtures;

@ExtendWith(MockitoExtension.class)
class WalletRepositoryAdapterTest {

    private static final byte[] ENCRYPTED_KEY_SHARE = {101, 102, 103};
    private static final byte[] ENCRYPTED_PEER_KEY_SHARE = {104, 105, 106};
    private static final byte[] ENCRYPTED_DEK = {21, 22, 23};
    private static final byte[] KEY_SHARE_IV = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final byte[] PEER_KEY_SHARE_IV = {13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24};

    @Mock
    private WalletJpaRepository jpaRepository;

    @Spy
    private WalletEntityMapper mapper = new WalletEntityMapperImpl();

    @Mock
    private KeyShareEncryptor keyShareEncryptor;

    @Captor
    private ArgumentCaptor<WalletEntity> entityCaptor;

    @InjectMocks
    private WalletRepositoryAdapter adapter;

    @Test
    void shouldEncryptKeySharesOnFirstSave() {
        // given
        var wallet = WalletFixtures.walletBuilder().id(null).build();
        var encryptedMaterial = EncryptedKeyMaterial.builder()
                .encryptedKeyShareData(ENCRYPTED_KEY_SHARE)
                .encryptedPeerKeyShareData(ENCRYPTED_PEER_KEY_SHARE)
                .encryptedDek(ENCRYPTED_DEK)
                .keyShareIv(KEY_SHARE_IV)
                .peerKeyShareIv(PEER_KEY_SHARE_IV)
                .build();
        var decryptedMaterial = DecryptedKeyMaterial.builder()
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .build();
        given(keyShareEncryptor.encrypt(SOME_KEY_SHARE_DATA, SOME_PEER_KEY_SHARE_DATA))
                .willReturn(encryptedMaterial);
        given(keyShareEncryptor.decrypt(
                ENCRYPTED_KEY_SHARE, ENCRYPTED_PEER_KEY_SHARE,
                ENCRYPTED_DEK, KEY_SHARE_IV, PEER_KEY_SHARE_IV))
                .willReturn(decryptedMaterial);
        given(jpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = adapter.save(wallet);

        // then
        then(keyShareEncryptor).should().encrypt(SOME_KEY_SHARE_DATA, SOME_PEER_KEY_SHARE_DATA);
        var expected = WalletFixtures.walletBuilder().id(null).build();
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void shouldSkipEncryptionOnSubsequentSave() {
        // given
        var wallet = WalletFixtures.walletBuilder().build();
        var existingEntity = buildEncryptedEntity();
        given(jpaRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(existingEntity));
        given(jpaRepository.save(entityCaptor.capture()))
                .willAnswer(invocation -> invocation.getArgument(0));
        var decryptedMaterial = DecryptedKeyMaterial.builder()
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .build();
        given(keyShareEncryptor.decrypt(
                ENCRYPTED_KEY_SHARE, ENCRYPTED_PEER_KEY_SHARE,
                ENCRYPTED_DEK, KEY_SHARE_IV, PEER_KEY_SHARE_IV))
                .willReturn(decryptedMaterial);

        // when
        adapter.save(wallet);

        // then
        then(keyShareEncryptor).should(never()).encrypt(SOME_KEY_SHARE_DATA, SOME_PEER_KEY_SHARE_DATA);
        var savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getKeyShareDek()).isEqualTo(ENCRYPTED_DEK);
    }

    @Test
    void shouldDecryptKeySharesOnFind() {
        // given
        var entity = buildEncryptedEntity();
        var decryptedMaterial = DecryptedKeyMaterial.builder()
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .build();
        given(jpaRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(entity));
        given(keyShareEncryptor.decrypt(
                ENCRYPTED_KEY_SHARE, ENCRYPTED_PEER_KEY_SHARE,
                ENCRYPTED_DEK, KEY_SHARE_IV, PEER_KEY_SHARE_IV))
                .willReturn(decryptedMaterial);

        // when
        var result = adapter.findById(SOME_WALLET_ID);

        // then
        var expected = WalletFixtures.walletBuilder().build();
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldSkipDecryptionWhenDekIsNull() {
        // given
        var entity = mapper.toEntity(WalletFixtures.walletBuilder().build());
        given(jpaRepository.findBySolanaAddress(SOME_SOLANA_ADDRESS)).willReturn(Optional.of(entity));

        // when
        var result = adapter.findBySolanaAddress(SOME_SOLANA_ADDRESS);

        // then
        var expected = WalletFixtures.walletBuilder().build();
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().isEqualTo(expected);
        then(keyShareEncryptor).shouldHaveNoInteractions();
    }

    @Test
    void shouldDecryptOnFindByUserId() {
        // given
        var entity = buildEncryptedEntity();
        var decryptedMaterial = DecryptedKeyMaterial.builder()
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .build();
        given(jpaRepository.findByUserId(SOME_USER_ID)).willReturn(Optional.of(entity));
        given(keyShareEncryptor.decrypt(
                ENCRYPTED_KEY_SHARE, ENCRYPTED_PEER_KEY_SHARE,
                ENCRYPTED_DEK, KEY_SHARE_IV, PEER_KEY_SHARE_IV))
                .willReturn(decryptedMaterial);

        // when
        var result = adapter.findByUserId(SOME_USER_ID);

        // then
        var expected = WalletFixtures.walletBuilder().build();
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    private WalletEntity buildEncryptedEntity() {
        var entity = mapper.toEntity(WalletFixtures.walletBuilder().build());
        entity.setKeyShareData(ENCRYPTED_KEY_SHARE);
        entity.setPeerKeyShareData(ENCRYPTED_PEER_KEY_SHARE);
        entity.setKeyShareDek(ENCRYPTED_DEK);
        entity.setKeyShareIv(KEY_SHARE_IV);
        entity.setPeerKeyShareIv(PEER_KEY_SHARE_IV);
        return entity;
    }
}
