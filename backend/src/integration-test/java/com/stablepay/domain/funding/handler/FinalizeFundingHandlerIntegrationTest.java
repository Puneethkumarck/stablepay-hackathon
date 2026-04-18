package com.stablepay.domain.funding.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.PgTest;

@PgTest
class FinalizeFundingHandlerIntegrationTest {

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("0.000000");
    private static final BigDecimal FUNDING_AMOUNT = new BigDecimal("25.000000");
    private static final String[] WALLET_IGNORED_FIELDS =
            {"id", "createdAt", "updatedAt"};
    private static final String[] ORDER_IGNORED_FIELDS =
            {"id", "createdAt", "updatedAt"};

    @Autowired
    private FinalizeFundingHandler handler;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FundingOrderRepository fundingOrderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Wallet initialWallet;
    private FundingOrder initialOrder;

    @BeforeEach
    void setUp() {
        var unique = String.valueOf(System.nanoTime());
        transactionTemplate.executeWithoutResult(status -> {
            initialWallet = walletRepository.save(Wallet.builder()
                    .userId("finalize-user-" + unique)
                    .solanaAddress("finalize-addr-" + unique)
                    .publicKey(new byte[]{1})
                    .keyShareData(new byte[]{2})
                    .peerKeyShareData(new byte[]{3})
                    .availableBalance(INITIAL_BALANCE)
                    .totalBalance(INITIAL_BALANCE)
                    .build());
            initialOrder = fundingOrderRepository.save(FundingOrder.builder()
                    .fundingId(UUID.randomUUID())
                    .walletId(initialWallet.id())
                    .amountUsdc(FUNDING_AMOUNT)
                    .stripePaymentIntentId("pi_finalize_" + unique)
                    .status(FundingStatus.PAYMENT_CONFIRMED)
                    .build());
        });
    }

    @Test
    void shouldIncrementBalanceAndFlipToFundedOnFirstInvocation() {
        // when
        handler.handle(initialOrder.fundingId(), initialWallet.id(), FUNDING_AMOUNT);

        // then
        var expectedWallet = initialWallet.toBuilder()
                .availableBalance(FUNDING_AMOUNT)
                .totalBalance(FUNDING_AMOUNT)
                .build();
        assertThat(reloadWallet())
                .usingRecursiveComparison()
                .ignoringFields(WALLET_IGNORED_FIELDS)
                .isEqualTo(expectedWallet);

        var expectedOrder = initialOrder.toBuilder().status(FundingStatus.FUNDED).build();
        assertThat(reloadOrder())
                .usingRecursiveComparison()
                .ignoringFields(ORDER_IGNORED_FIELDS)
                .isEqualTo(expectedOrder);
    }

    @Test
    void shouldBeIdempotentWhenRetriedAfterSuccessfulCommit() {
        // given — first call commits in its own transaction
        handler.handle(initialOrder.fundingId(), initialWallet.id(), FUNDING_AMOUNT);

        // when — second call (Temporal retry after ack lost) in a new transaction
        handler.handle(initialOrder.fundingId(), initialWallet.id(), FUNDING_AMOUNT);

        // then — balance incremented exactly once, status stays FUNDED
        var expectedWallet = initialWallet.toBuilder()
                .availableBalance(FUNDING_AMOUNT)
                .totalBalance(FUNDING_AMOUNT)
                .build();
        assertThat(reloadWallet())
                .usingRecursiveComparison()
                .ignoringFields(WALLET_IGNORED_FIELDS)
                .isEqualTo(expectedWallet);

        var expectedOrder = initialOrder.toBuilder().status(FundingStatus.FUNDED).build();
        assertThat(reloadOrder())
                .usingRecursiveComparison()
                .ignoringFields(ORDER_IGNORED_FIELDS)
                .isEqualTo(expectedOrder);
    }

    private Wallet reloadWallet() {
        return transactionTemplate.execute(
                status -> walletRepository.findById(initialWallet.id()).orElseThrow());
    }

    private FundingOrder reloadOrder() {
        return transactionTemplate.execute(
                status -> fundingOrderRepository.findByFundingId(initialOrder.fundingId())
                        .orElseThrow());
    }
}
