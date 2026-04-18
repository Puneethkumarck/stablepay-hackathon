package com.stablepay.domain.funding.handler;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
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
        // given — regression guard for the actual failure mode observed in STA-82:
        // when CompleteFundingHandler ran inside a read-only Spring tx, WalletRepository
        // .findById issued SELECT ... FOR NO KEY UPDATE (because the JpaRepository had
        // a global @Lock), and PostgreSQL refused it with "cannot execute SELECT FOR
        // NO KEY UPDATE in a read-only transaction". We drive the handler explicitly
        // inside a read-only transaction here so the assertion does not depend on the
        // handler's own @Transactional metadata staying unchanged.
        var readOnlyTemplate = new TransactionTemplate(transactionTemplate.getTransactionManager());
        readOnlyTemplate.setReadOnly(true);
        readOnlyTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // when / then
        assertThatCode(() ->
                readOnlyTemplate.executeWithoutResult(
                        status -> handler.handle(confirmedOrder.fundingId())))
                .doesNotThrowAnyException();
    }
}
