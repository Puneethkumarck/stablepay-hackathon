package com.stablepay.domain.funding.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.PgTest;

@PgTest
@Transactional
class FinalizeFundingHandlerIntegrationTest {

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("0.000000");
    private static final BigDecimal FUNDING_AMOUNT = new BigDecimal("25.000000");

    @Autowired
    private FinalizeFundingHandler handler;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FundingOrderRepository fundingOrderRepository;

    private Long walletId;
    private UUID fundingId;

    @BeforeEach
    void setUp() {
        var unique = String.valueOf(System.nanoTime());
        var wallet = Wallet.builder()
                .userId("finalize-user-" + unique)
                .solanaAddress("finalize-addr-" + unique)
                .availableBalance(INITIAL_BALANCE)
                .totalBalance(INITIAL_BALANCE)
                .build();
        walletId = walletRepository.save(wallet).id();

        fundingId = UUID.randomUUID();
        fundingOrderRepository.save(FundingOrder.builder()
                .fundingId(fundingId)
                .walletId(walletId)
                .amountUsdc(FUNDING_AMOUNT)
                .stripePaymentIntentId("pi_finalize_" + unique)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build());
    }

    @Test
    void shouldIncrementBalanceAndFlipToFundedOnFirstInvocation() {
        // when
        handler.handle(fundingId, walletId, FUNDING_AMOUNT);

        // then
        var reloadedWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(reloadedWallet.availableBalance()).isEqualByComparingTo(FUNDING_AMOUNT);
        assertThat(reloadedWallet.totalBalance()).isEqualByComparingTo(FUNDING_AMOUNT);

        var reloadedOrder = fundingOrderRepository.findByFundingId(fundingId).orElseThrow();
        assertThat(reloadedOrder.status()).isEqualTo(FundingStatus.FUNDED);
    }

    @Test
    void shouldBeIdempotentWhenRetriedAfterSuccessfulCommit() {
        // given — first successful invocation
        handler.handle(fundingId, walletId, FUNDING_AMOUNT);

        // when — retry (e.g. Temporal ack lost after commit)
        handler.handle(fundingId, walletId, FUNDING_AMOUNT);

        // then — balance incremented exactly once, status remains FUNDED
        var reloadedWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(reloadedWallet.availableBalance()).isEqualByComparingTo(FUNDING_AMOUNT);
        assertThat(reloadedWallet.totalBalance()).isEqualByComparingTo(FUNDING_AMOUNT);

        var reloadedOrder = fundingOrderRepository.findByFundingId(fundingId).orElseThrow();
        assertThat(reloadedOrder.status()).isEqualTo(FundingStatus.FUNDED);
    }
}
