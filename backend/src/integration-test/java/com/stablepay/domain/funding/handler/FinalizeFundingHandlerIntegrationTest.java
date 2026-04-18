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

// No class-level @Transactional: each handler invocation must commit in its own
// transaction so the retry in shouldBeIdempotentWhenRetriedAfterSuccessfulCommit
// actually exercises the post-commit idempotency guard (status = FUNDED) rather
// than reading dirty in-session state from the same outer transaction.
@PgTest
class FinalizeFundingHandlerIntegrationTest {

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("0.000000");
    private static final BigDecimal FUNDING_AMOUNT = new BigDecimal("25.000000");

    @Autowired
    private FinalizeFundingHandler handler;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FundingOrderRepository fundingOrderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long walletId;
    private UUID fundingId;

    @BeforeEach
    void setUp() {
        var unique = String.valueOf(System.nanoTime());
        transactionTemplate.executeWithoutResult(status -> {
            var saved = walletRepository.save(Wallet.builder()
                    .userId("finalize-user-" + unique)
                    .solanaAddress("finalize-addr-" + unique)
                    .availableBalance(INITIAL_BALANCE)
                    .totalBalance(INITIAL_BALANCE)
                    .build());
            walletId = saved.id();

            fundingId = UUID.randomUUID();
            fundingOrderRepository.save(FundingOrder.builder()
                    .fundingId(fundingId)
                    .walletId(walletId)
                    .amountUsdc(FUNDING_AMOUNT)
                    .stripePaymentIntentId("pi_finalize_" + unique)
                    .status(FundingStatus.PAYMENT_CONFIRMED)
                    .build());
        });
    }

    @Test
    void shouldIncrementBalanceAndFlipToFundedOnFirstInvocation() {
        // when
        handler.handle(fundingId, walletId, FUNDING_AMOUNT);

        // then
        var reloadedWallet = reloadWallet();
        assertThat(reloadedWallet.availableBalance()).isEqualByComparingTo(FUNDING_AMOUNT);
        assertThat(reloadedWallet.totalBalance()).isEqualByComparingTo(FUNDING_AMOUNT);
        assertThat(reloadOrder().status()).isEqualTo(FundingStatus.FUNDED);
    }

    @Test
    void shouldBeIdempotentWhenRetriedAfterSuccessfulCommit() {
        // given — first call commits in its own transaction
        handler.handle(fundingId, walletId, FUNDING_AMOUNT);

        // when — second call (Temporal retry after ack lost) in a new transaction
        handler.handle(fundingId, walletId, FUNDING_AMOUNT);

        // then — balance incremented exactly once, status stays FUNDED
        var reloadedWallet = reloadWallet();
        assertThat(reloadedWallet.availableBalance()).isEqualByComparingTo(FUNDING_AMOUNT);
        assertThat(reloadedWallet.totalBalance()).isEqualByComparingTo(FUNDING_AMOUNT);
        assertThat(reloadOrder().status()).isEqualTo(FundingStatus.FUNDED);
    }

    private Wallet reloadWallet() {
        return transactionTemplate.execute(
                status -> walletRepository.findById(walletId).orElseThrow());
    }

    private FundingOrder reloadOrder() {
        return transactionTemplate.execute(
                status -> fundingOrderRepository.findByFundingId(fundingId).orElseThrow());
    }
}
