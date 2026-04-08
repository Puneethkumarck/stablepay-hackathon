package com.stablepay.infrastructure.db.wallet;

import static com.stablepay.testutil.WalletFixtures.SOME_BALANCE;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.PgTest;

@PgTest
@Transactional
class WalletRepositoryIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void shouldSaveAndFindWalletById() {
        // given
        var wallet = Wallet.builder()
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
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
        var wallet = Wallet.builder()
                .userId("unique-user-" + System.nanoTime())
                .solanaAddress("addr-" + System.nanoTime())
                .availableBalance(SOME_BALANCE)
                .totalBalance(SOME_BALANCE)
                .build();
        walletRepository.save(wallet);

        // when
        var found = walletRepository.findByUserId(wallet.userId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(wallet.userId());
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
        var solanaAddress = "addr-solana-" + System.nanoTime();
        var wallet = Wallet.builder()
                .userId("user-solana-" + System.nanoTime())
                .solanaAddress(solanaAddress)
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
}
