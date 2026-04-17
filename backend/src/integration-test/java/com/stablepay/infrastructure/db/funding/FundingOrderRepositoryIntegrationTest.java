package com.stablepay.infrastructure.db.funding;

import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
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

    @Nested
    class Save {

        @Test
        void shouldSaveAndAssignId() {
            // given
            var walletId = createWalletAndReturnId();
            var order = fundingOrderBuilder(walletId)
                    .stripePaymentIntentId("pi_save_" + System.nanoTime())
                    .build();

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
            var saved = fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .stripePaymentIntentId("pi_status_" + System.nanoTime())
                    .build());

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
            var saved = fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .stripePaymentIntentId("pi_findfid_" + System.nanoTime())
                    .build());

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
            var saved = fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .stripePaymentIntentId(paymentIntentId)
                    .build());

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
            var confirmed = fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .stripePaymentIntentId("pi_in_confirmed_" + System.nanoTime())
                    .build());
            var funded = fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .status(FundingStatus.FUNDED)
                    .stripePaymentIntentId("pi_in_funded_" + System.nanoTime())
                    .build());
            fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .status(FundingStatus.FAILED)
                    .stripePaymentIntentId("pi_in_failed_" + System.nanoTime())
                    .build());

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
            fundingOrderRepository.save(fundingOrderBuilder(otherWalletId)
                    .stripePaymentIntentId("pi_other_" + System.nanoTime())
                    .build());

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
            fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .stripePaymentIntentId("pi_active_a_" + System.nanoTime())
                    .build());
            entityManager.flush();
            var duplicate = fundingOrderBuilder(walletId)
                    .stripePaymentIntentId("pi_active_b_" + System.nanoTime())
                    .build();

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
            fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .status(FundingStatus.FUNDED)
                    .stripePaymentIntentId("pi_inactive_a_" + System.nanoTime())
                    .build());
            fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .status(FundingStatus.FAILED)
                    .stripePaymentIntentId("pi_inactive_b_" + System.nanoTime())
                    .build());
            fundingOrderRepository.save(fundingOrderBuilder(walletId)
                    .status(FundingStatus.REFUNDED)
                    .stripePaymentIntentId("pi_inactive_c_" + System.nanoTime())
                    .build());

            // when
            entityManager.flush();
            var found = fundingOrderRepository.findByWalletIdAndStatusIn(
                    walletId, List.of(FundingStatus.FUNDED, FundingStatus.FAILED, FundingStatus.REFUNDED));

            // then
            assertThat(found).hasSize(3);
        }
    }
}
