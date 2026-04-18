package com.stablepay.domain.funding.handler;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * Persists funding-order state changes inside dedicated REQUIRES_NEW transactions
 * so external I/O (Stripe API calls) is never held inside a DB transaction.
 *
 * <p>The Stripe payment_succeeded webhook can fire before the caller's outer
 * request returns. If the funding row hasn't committed by then, the webhook
 * handler can't see it and silently drops the event, stalling the state machine
 * at PAYMENT_CONFIRMED. Splitting persistence into its own committed transaction
 * guarantees the row is visible to other transactions before any external call.
 */
@Service
@RequiredArgsConstructor
public class FundingOrderWriter {

    private final FundingOrderRepository fundingOrderRepository;

    /**
     * The DB enforces at most one PAYMENT_CONFIRMED order per wallet via a
     * partial unique index (V5 migration). A constraint violation here means
     * a concurrent funding is already active.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FundingOrder savePending(Long walletId, BigDecimal amount) {
        try {
            return fundingOrderRepository.save(FundingOrder.builder()
                    .fundingId(UUID.randomUUID())
                    .walletId(walletId)
                    .amountUsdc(amount)
                    .status(FundingStatus.PAYMENT_CONFIRMED)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw FundingAlreadyInProgressException.forWallet(walletId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FundingOrder attachPaymentIntent(FundingOrder order, String paymentIntentId) {
        return fundingOrderRepository.save(order.toBuilder()
                .stripePaymentIntentId(paymentIntentId)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(FundingOrder order) {
        fundingOrderRepository.save(order.toBuilder()
                .status(FundingStatus.FAILED)
                .build());
    }
}
