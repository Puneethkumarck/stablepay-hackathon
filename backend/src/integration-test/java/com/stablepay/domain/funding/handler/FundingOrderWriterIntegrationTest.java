package com.stablepay.domain.funding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.PgTest;

@PgTest
class FundingOrderWriterIntegrationTest {

    private static final BigDecimal ZERO_BALANCE = new BigDecimal("0.000000");
    private static final BigDecimal AMOUNT = new BigDecimal("25.000000");

    @Autowired
    private FundingOrderWriter fundingOrderWriter;

    @Autowired
    private FundingOrderRepository fundingOrderRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long walletId;

    @BeforeEach
    void setUp() {
        var unique = String.valueOf(System.nanoTime());
        walletId = transactionTemplate.execute(status -> walletRepository.save(Wallet.builder()
                .userId("writer-user-" + unique)
                .solanaAddress("writer-addr-" + unique)
                .publicKey(new byte[]{1})
                .keyShareData(new byte[]{2})
                .peerKeyShareData(new byte[]{3})
                .availableBalance(ZERO_BALANCE)
                .totalBalance(ZERO_BALANCE)
                .build())).id();
    }

    @Test
    void shouldCommitPendingOrderImmediatelySoOtherTransactionsCanReadIt() {
        // when — savePending runs in REQUIRES_NEW; the row must be visible to a
        // brand-new read transaction started AFTER savePending returns.
        var saved = fundingOrderWriter.savePending(walletId, AMOUNT);

        // then
        var reloaded = transactionTemplate.execute(status ->
                fundingOrderRepository.findByFundingId(saved.fundingId()).orElseThrow());
        assertThat(reloaded.status()).isEqualTo(FundingStatus.PAYMENT_CONFIRMED);
        assertThat(reloaded.walletId()).isEqualTo(walletId);
        assertThat(reloaded.amountUsdc()).isEqualByComparingTo(AMOUNT);
    }

    @Test
    void shouldRejectSecondPendingOrderForSameWalletViaPartialUniqueIndex() {
        // given — first pending order is committed.
        fundingOrderWriter.savePending(walletId, AMOUNT);

        // when / then — second savePending hits the partial unique index
        // (idx_funding_orders_one_active_per_wallet on V5) and is translated.
        assertThatThrownBy(() -> fundingOrderWriter.savePending(walletId, AMOUNT))
                .isInstanceOf(FundingAlreadyInProgressException.class)
                .hasMessageContaining("SP-0022")
                .hasMessageContaining(walletId.toString());
    }

    @Test
    void shouldAllowNewPendingOrderAfterPreviousIsMarkedFailed() {
        // given
        var first = fundingOrderWriter.savePending(walletId, AMOUNT);
        fundingOrderWriter.markFailed(first);

        // when — partial index only constrains PAYMENT_CONFIRMED rows; FAILED
        // rows do not occupy the slot.
        var second = fundingOrderWriter.savePending(walletId, AMOUNT);

        // then
        assertThat(second.fundingId()).isNotEqualTo(first.fundingId());
        assertThat(second.status()).isEqualTo(FundingStatus.PAYMENT_CONFIRMED);
    }

    @Test
    void shouldAttachPaymentIntentToExistingPendingOrder() {
        // given
        var pending = fundingOrderWriter.savePending(walletId, AMOUNT);

        // when
        var withIntent = fundingOrderWriter.attachPaymentIntent(pending, "pi_writer_test");

        // then
        var reloaded = transactionTemplate.execute(status ->
                fundingOrderRepository.findByFundingId(withIntent.fundingId()).orElseThrow());
        assertThat(reloaded.stripePaymentIntentId()).isEqualTo("pi_writer_test");
        assertThat(reloaded.status()).isEqualTo(FundingStatus.PAYMENT_CONFIRMED);
    }
}
