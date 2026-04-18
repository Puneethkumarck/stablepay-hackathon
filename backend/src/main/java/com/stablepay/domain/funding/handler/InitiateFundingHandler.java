package com.stablepay.domain.funding.handler;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.model.FundingInitiationResult;
import com.stablepay.domain.funding.model.PaymentRequest;
import com.stablepay.domain.funding.port.PaymentGateway;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates wallet funding: persist a pending order, call Stripe, then
 * attach the payment-intent reference (or mark FAILED).
 *
 * <p>This class deliberately holds no DB transaction. Each persistence step is
 * delegated to {@link FundingOrderWriter} which wraps the write in a dedicated
 * REQUIRES_NEW transaction. The Stripe call sits between two independent,
 * already-committed writes so the payment_succeeded webhook always sees the
 * pending row even if it fires before this method returns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateFundingHandler {

    private final WalletRepository walletRepository;
    private final PaymentGateway paymentGateway;
    private final FundingOrderWriter fundingOrderWriter;

    public FundingInitiationResult handle(Long walletId, BigDecimal amount) {
        if (!walletRepository.existsById(walletId)) {
            throw WalletNotFoundException.byId(walletId);
        }

        var saved = fundingOrderWriter.savePending(walletId, amount);

        try {
            var paymentResult = paymentGateway.initiatePayment(PaymentRequest.builder()
                    .fundingId(saved.fundingId())
                    .walletId(walletId)
                    .amountUsdc(amount)
                    .build());
            var persisted = fundingOrderWriter.attachPaymentIntent(
                    saved, paymentResult.pspReference());
            log.info("Funding initiated fundingId={} walletId={} paymentIntentId={}",
                    persisted.fundingId(), walletId, paymentResult.pspReference());
            return FundingInitiationResult.builder()
                    .order(persisted)
                    .clientSecret(paymentResult.clientSecret())
                    .build();
        } catch (FundingFailedException e) {
            fundingOrderWriter.markFailed(saved);
            log.error("Funding failed fundingId={} walletId={}",
                    saved.fundingId(), walletId, e);
            throw e;
        }
    }
}
