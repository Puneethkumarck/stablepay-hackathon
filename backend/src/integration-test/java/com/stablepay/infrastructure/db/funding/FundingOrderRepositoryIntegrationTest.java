package com.stablepay.infrastructure.db.funding;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.PgTest;

@PgTest
@Transactional
class FundingOrderRepositoryIntegrationTest {

    @Autowired
    private FundingOrderRepository fundingOrderRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private EntityManager entityManager;

    private Long createWalletAndReturnId() {
        var unique = String.valueOf(System.nanoTime());
        var wallet = Wallet.builder()
                .userId("funding-user-" + unique)
                .solanaAddress("funding-addr-" + unique)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();
        return walletRepository.save(wallet).id();
    }

    private FundingOrder buildOrder(Long walletId, FundingStatus status, String paymentIntentId) {
        return FundingOrder.builder()
                .fundingId(UUID.randomUUID())
                .walletId(walletId)
                .amountUsdc(SOME_AMOUNT_USDC)
                .stripePaymentIntentId(paymentIntentId)
                .status(status)
                .build();
    }

    @Nested
    class Save {

        @Test
        void shouldSaveAndAssignId() {
            // given
            var walletId = createWalletAndReturnId();
            var order = buildOrder(walletId, FundingStatus.PAYMENT_CONFIRMED, "pi_save_" + System.nanoTime());

            // when
            var saved = fundingOrderRepository.save(order);

            // then
            assertThat(saved.id()).isNotNull();
            assertThat(saved)
                    .usingRecursiveComparison()
                    .ignoringFields("id", "createdAt", "updatedAt")
                    .isEqualTo(order);
        }

        @Test
        void shouldPersistStatusUpdates() {
            // given
            var walletId = createWalletAndReturnId();
            var saved = fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.PAYMENT_CONFIRMED, "pi_status_" + System.nanoTime()));

            // when
            fundingOrderRepository.save(saved.toBuilder().status(FundingStatus.FUNDED).build());

            // then
            var found = fundingOrderRepository.findByFundingId(saved.fundingId());
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo(FundingStatus.FUNDED);
        }
    }

    @Nested
    class FindByFundingId {

        @Test
        void shouldFindOrderByFundingId() {
            // given
            var walletId = createWalletAndReturnId();
            var order = buildOrder(walletId, FundingStatus.PAYMENT_CONFIRMED, "pi_findfid_" + System.nanoTime());
            var saved = fundingOrderRepository.save(order);

            // when
            var found = fundingOrderRepository.findByFundingId(saved.fundingId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get())
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt", "updatedAt")
                    .isEqualTo(saved);
        }

        @Test
        void shouldReturnEmptyWhenFundingIdNotFound() {
            // when
            var found = fundingOrderRepository.findByFundingId(UUID.randomUUID());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    class FindByStripePaymentIntentId {

        @Test
        void shouldFindOrderByStripePaymentIntentId() {
            // given
            var walletId = createWalletAndReturnId();
            var paymentIntentId = "pi_findpi_" + System.nanoTime();
            var saved = fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.PAYMENT_CONFIRMED, paymentIntentId));

            // when
            var found = fundingOrderRepository.findByStripePaymentIntentId(paymentIntentId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get())
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt", "updatedAt")
                    .isEqualTo(saved);
        }

        @Test
        void shouldReturnEmptyWhenPaymentIntentIdNotFound() {
            // when
            var found = fundingOrderRepository.findByStripePaymentIntentId("pi_nonexistent");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    class FindByWalletIdAndStatusIn {

        @Test
        void shouldReturnOrdersMatchingWalletAndStatuses() {
            // given
            var walletId = createWalletAndReturnId();
            var confirmed = fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.PAYMENT_CONFIRMED, "pi_in_confirmed_" + System.nanoTime()));
            var funded = fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.FUNDED, "pi_in_funded_" + System.nanoTime()));
            fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.FAILED, "pi_in_failed_" + System.nanoTime()));

            // when
            var found = fundingOrderRepository.findByWalletIdAndStatusIn(
                    walletId, List.of(FundingStatus.PAYMENT_CONFIRMED, FundingStatus.FUNDED));

            // then
            assertThat(found)
                    .extracting(FundingOrder::id)
                    .containsExactlyInAnyOrder(confirmed.id(), funded.id());
        }

        @Test
        void shouldNotReturnOrdersFromOtherWallets() {
            // given
            var walletId = createWalletAndReturnId();
            var otherWalletId = createWalletAndReturnId();
            fundingOrderRepository.save(
                    buildOrder(otherWalletId, FundingStatus.PAYMENT_CONFIRMED, "pi_other_" + System.nanoTime()));

            // when
            var found = fundingOrderRepository.findByWalletIdAndStatusIn(
                    walletId, List.of(FundingStatus.PAYMENT_CONFIRMED));

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    class PartialUniqueIndex {

        @Test
        void shouldRejectSecondActiveOrderForSameWallet() {
            // given
            var walletId = createWalletAndReturnId();
            fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.PAYMENT_CONFIRMED, "pi_active_a_" + System.nanoTime()));
            entityManager.flush();
            var duplicate = buildOrder(
                    walletId, FundingStatus.PAYMENT_CONFIRMED, "pi_active_b_" + System.nanoTime());

            // when / then
            assertThatThrownBy(() -> {
                fundingOrderRepository.save(duplicate);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void shouldAllowMultipleNonActiveOrdersForSameWallet() {
            // given
            var walletId = createWalletAndReturnId();
            fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.FUNDED, "pi_inactive_a_" + System.nanoTime()));
            fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.FAILED, "pi_inactive_b_" + System.nanoTime()));
            fundingOrderRepository.save(
                    buildOrder(walletId, FundingStatus.REFUNDED, "pi_inactive_c_" + System.nanoTime()));

            // when
            entityManager.flush();
            var found = fundingOrderRepository.findByWalletIdAndStatusIn(
                    walletId, List.of(FundingStatus.FUNDED, FundingStatus.FAILED, FundingStatus.REFUNDED));

            // then
            assertThat(found).hasSize(3);
        }
    }
}
