package com.stablepay.infrastructure.db.wallet;

import static com.stablepay.testutil.WalletFixtures.SOME_BALANCE;
import static com.stablepay.testutil.WalletFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PEER_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PUBLIC_KEY;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.PgTest;

@PgTest
@Transactional
class WalletRepositoryIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID createUser() {
        var userId = UUID.randomUUID();
        userRepository.save(AppUser.builder().id(userId).email(userId + "@test.com").build());
        return userId;
    }

    @Test
    void shouldSaveAndFindWalletById() {
        // given
        var userId = createUser();
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        // when
        var saved = walletRepository.save(wallet);
        var found = walletRepository.findById(saved.id());

        // then
        assertThat(found).isPresent();
        assertThat(found.get())
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(saved);
    }

    @Test
    void shouldFindWalletByUserId() {
        // given
        var userId = createUser();
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress("addr-" + System.nanoTime())
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(SOME_BALANCE)
                .totalBalance(SOME_BALANCE)
                .build();
        walletRepository.save(wallet);

        // when
        var found = walletRepository.findByUserId(userId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(userId);
    }

    @Test
    void shouldReturnEmptyWhenWalletNotFound() {
        // when
        var found = walletRepository.findById(999999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindWalletBySolanaAddress() {
        // given
        var userId = createUser();
        var solanaAddress = "addr-solana-" + System.nanoTime();
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress(solanaAddress)
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(SOME_BALANCE)
                .totalBalance(SOME_BALANCE)
                .build();
        walletRepository.save(wallet);

        // when
        var found = walletRepository.findBySolanaAddress(solanaAddress);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().solanaAddress()).isEqualTo(solanaAddress);
    }

    @Test
    void shouldReturnEmptyWhenSolanaAddressNotFound() {
        // when
        var found = walletRepository.findBySolanaAddress("nonexistent-solana-address");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldRejectWalletWithNullPeerKeyShareData() {
        // given
        var userId = createUser();
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress("null-peer-addr-" + System.nanoTime())
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(null)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        // when / then
        assertThatThrownBy(() -> {
            walletRepository.save(wallet);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectWalletWithNullKeyShareData() {
        // given
        var userId = createUser();
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress("null-self-addr-" + System.nanoTime())
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(null)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        // when / then
        assertThatThrownBy(() -> {
            walletRepository.save(wallet);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectWalletWithNullPublicKey() {
        // given
        var userId = createUser();
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress("null-pk-addr-" + System.nanoTime())
                .publicKey(null)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        // when / then
        assertThatThrownBy(() -> {
            walletRepository.save(wallet);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
