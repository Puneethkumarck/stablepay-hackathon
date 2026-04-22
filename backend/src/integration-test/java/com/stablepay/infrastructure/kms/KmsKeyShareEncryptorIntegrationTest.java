package com.stablepay.infrastructure.kms;

import static com.stablepay.testutil.WalletFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PEER_KEY_SHARE_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.LocalStackKmsContainerExtension;
import com.stablepay.test.PgTest;
import com.stablepay.testutil.AuthFixtures;
import com.stablepay.testutil.WalletFixtures;

@PgTest
@ExtendWith(LocalStackKmsContainerExtension.class)
@Transactional
class KmsKeyShareEncryptorIntegrationTest {

    @DynamicPropertySource
    static void kmsProperties(DynamicPropertyRegistry registry) {
        registry.add("stablepay.kms.enabled", () -> "true");
        registry.add("stablepay.kms.key-arn", LocalStackKmsContainerExtension::getKeyArn);
        registry.add("stablepay.kms.endpoint", LocalStackKmsContainerExtension::getEndpoint);
        registry.add("stablepay.kms.region", LocalStackKmsContainerExtension::getRegion);
    }

    @Autowired
    private KeyShareEncryptor keyShareEncryptor;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldEncryptAndDecryptKeySharesRoundTrip() {
        // given
        byte[] keyShare = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] peerKeyShare = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

        // when
        var encrypted = keyShareEncryptor.encrypt(keyShare, peerKeyShare);
        var decrypted = keyShareEncryptor.decrypt(
                encrypted.encryptedKeyShareData(), encrypted.encryptedPeerKeyShareData(),
                encrypted.encryptedDek(), encrypted.keyShareIv(), encrypted.peerKeyShareIv());

        // then
        assertThat(decrypted.keyShareData()).isEqualTo(keyShare);
        assertThat(decrypted.peerKeyShareData()).isEqualTo(peerKeyShare);
    }

    @Test
    void shouldProduceUniqueDeksPerEncryptCall() {
        // given
        byte[] keyShare = {1, 2, 3};
        byte[] peerKeyShare = {4, 5, 6};

        // when
        var first = keyShareEncryptor.encrypt(keyShare, peerKeyShare);
        var second = keyShareEncryptor.encrypt(keyShare, peerKeyShare);

        // then
        assertThat(first.encryptedDek()).isNotEqualTo(second.encryptedDek());
        assertThat(first.keyShareIv()).isNotEqualTo(second.keyShareIv());
    }

    @Test
    void shouldFailDecryptWithTamperedCiphertext() {
        // given
        byte[] keyShare = {1, 2, 3};
        byte[] peerKeyShare = {4, 5, 6};
        var encrypted = keyShareEncryptor.encrypt(keyShare, peerKeyShare);
        byte[] tampered = encrypted.encryptedKeyShareData().clone();
        tampered[0] ^= 0xFF;

        // when / then
        assertThatThrownBy(() -> keyShareEncryptor.decrypt(
                tampered, encrypted.encryptedPeerKeyShareData(),
                encrypted.encryptedDek(), encrypted.keyShareIv(), encrypted.peerKeyShareIv()))
                .isInstanceOf(KeyShareEncryptionException.class);
    }

    @Test
    void shouldTransparentlyEncryptOnSaveAndDecryptOnFind() {
        // given
        var userId = AuthFixtures.createTestUser(userRepository);
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress("kms-test-" + System.nanoTime())
                .publicKey(WalletFixtures.SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        // when
        var saved = walletRepository.save(wallet);
        entityManager.flush();
        entityManager.clear();
        var found = walletRepository.findById(saved.id());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().keyShareData()).isEqualTo(SOME_KEY_SHARE_DATA);
        assertThat(found.get().peerKeyShareData()).isEqualTo(SOME_PEER_KEY_SHARE_DATA);
    }

    @Test
    void shouldStoreEncryptedDataInDatabase() {
        // given
        var userId = AuthFixtures.createTestUser(userRepository);
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress("kms-raw-" + System.nanoTime())
                .publicKey(WalletFixtures.SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();
        var saved = walletRepository.save(wallet);
        entityManager.flush();
        entityManager.clear();

        // when
        var rawKeyShare = (byte[]) entityManager
                .createNativeQuery("SELECT key_share_data FROM wallets WHERE id = :id")
                .setParameter("id", saved.id())
                .getSingleResult();
        var rawDek = (byte[]) entityManager
                .createNativeQuery("SELECT key_share_dek FROM wallets WHERE id = :id")
                .setParameter("id", saved.id())
                .getSingleResult();

        // then
        assertThat(rawKeyShare).isNotEqualTo(SOME_KEY_SHARE_DATA);
        assertThat(rawDek).isNotNull();
    }
}
