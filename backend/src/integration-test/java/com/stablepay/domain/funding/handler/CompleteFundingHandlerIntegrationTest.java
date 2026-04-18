package com.stablepay.domain.funding.handler;

import static org.assertj.core.api.Assertions.assertThatCode;

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
class CompleteFundingHandlerIntegrationTest {

    private static final BigDecimal ZERO_BALANCE = new BigDecimal("0.000000");
    private static final BigDecimal FUNDING_AMOUNT = new BigDecimal("25.000000");

    @Autowired
    private CompleteFundingHandler handler;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FundingOrderRepository fundingOrderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private FundingOrder confirmedOrder;

    @BeforeEach
    void setUp() {
        var unique = String.valueOf(System.nanoTime());
        transactionTemplate.executeWithoutResult(status -> {
            var wallet = walletRepository.save(Wallet.builder()
                    .userId("complete-user-" + unique)
                    .solanaAddress("complete-addr-" + unique)
                    .publicKey(new byte[]{1})
                    .keyShareData(new byte[]{2})
                    .peerKeyShareData(new byte[]{3})
                    .availableBalance(ZERO_BALANCE)
                    .totalBalance(ZERO_BALANCE)
                    .build());
            confirmedOrder = fundingOrderRepository.save(FundingOrder.builder()
                    .fundingId(UUID.randomUUID())
                    .walletId(wallet.id())
                    .amountUsdc(FUNDING_AMOUNT)
                    .stripePaymentIntentId("pi_complete_" + unique)
                    .status(FundingStatus.PAYMENT_CONFIRMED)
                    .build());
        });
    }

    @Test
    void shouldHandlePaymentSucceededInsideReadOnlyTransactionWithoutAcquiringRowLock() {
        // given — CompleteFundingHandler runs with @Transactional(readOnly = true).
        // Regression guard: if WalletRepository.findById were to issue SELECT ... FOR UPDATE
        // (as it did before splitting findByIdForUpdate), PostgreSQL would fail with
        // "cannot execute SELECT FOR NO KEY UPDATE in a read-only transaction"
        // and every payment_intent.succeeded webhook would 500.

        // when / then
        assertThatCode(() -> handler.handle(confirmedOrder.fundingId()))
                .doesNotThrowAnyException();
    }
}
