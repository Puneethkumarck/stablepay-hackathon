package com.stablepay.domain.funding.handler;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.model.FundingInitiationResult;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.model.PaymentRequest;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.funding.port.PaymentGateway;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = FundingFailedException.class)
public class InitiateFundingHandler {

    private final WalletRepository walletRepository;
    private final PaymentGateway paymentGateway;
    private final FundingOrderRepository fundingOrderRepository;

    public FundingInitiationResult handle(Long walletId, BigDecimal amount) {
        walletRepository.findById(walletId)
                .orElseThrow(() -> WalletNotFoundException.byId(walletId));

        var activeOrders = fundingOrderRepository.findByWalletIdAndStatusIn(
                walletId, List.of(FundingStatus.PAYMENT_CONFIRMED));
        if (!activeOrders.isEmpty()) {
            throw FundingAlreadyInProgressException.forWallet(walletId);
        }

        var pending = FundingOrder.builder()
                .fundingId(UUID.randomUUID())
                .walletId(walletId)
                .amountUsdc(amount)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build();

        FundingOrder saved;
        try {
            saved = fundingOrderRepository.save(pending);
        } catch (DataIntegrityViolationException e) {
            log.info("Funding conflict detected for walletId={} — another order is in progress", walletId);
            throw FundingAlreadyInProgressException.forWallet(walletId);
        }

        try {
            var paymentResult = paymentGateway.initiatePayment(PaymentRequest.builder()
                    .fundingId(saved.fundingId())
                    .walletId(walletId)
                    .amountUsdc(amount)
                    .build());
            var withPaymentIntent = saved.toBuilder()
                    .stripePaymentIntentId(paymentResult.pspReference())
                    .build();
            var persisted = fundingOrderRepository.save(withPaymentIntent);
            log.info("Funding initiated fundingId={} walletId={} paymentIntentId={}",
                    persisted.fundingId(), walletId, paymentResult.pspReference());
            return FundingInitiationResult.builder()
                    .order(persisted)
                    .clientSecret(paymentResult.clientSecret())
                    .build();
        } catch (FundingFailedException e) {
            var failed = saved.toBuilder().status(FundingStatus.FAILED).build();
            fundingOrderRepository.save(failed);
            log.warn("Funding failed fundingId={} walletId={}: {}",
                    saved.fundingId(), walletId, e.getMessage());
            throw e;
        }
    }
}
