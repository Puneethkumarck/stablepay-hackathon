package com.stablepay.domain.funding.handler;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.exception.InsufficientBalanceForRefundException;
import com.stablepay.domain.funding.exception.RefundFailedException;
import com.stablepay.domain.funding.exception.RefundNotAllowedException;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.funding.port.PaymentGateway;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.TreasuryService;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RefundFailedException.class)
public class RefundFundingHandler {

    private final FundingOrderRepository fundingOrderRepository;
    private final WalletRepository walletRepository;
    private final TreasuryService treasuryService;
    private final PaymentGateway paymentGateway;

    public FundingOrder handle(UUID fundingId) {
        requireNonNull(fundingId, "fundingId cannot be null");

        var order = fundingOrderRepository.findByFundingId(fundingId)
                .orElseThrow(() -> FundingOrderNotFoundException.byFundingId(fundingId));

        if (order.status() != FundingStatus.FUNDED) {
            throw RefundNotAllowedException.forStatus(order.status());
        }

        var wallet = walletRepository.findById(order.walletId())
                .orElseThrow(() -> WalletNotFoundException.byId(order.walletId()));

        var amount = order.amountUsdc();
        assertSufficientBalance(wallet, amount);

        var refundInitiated = order.toBuilder().status(FundingStatus.REFUND_INITIATED).build();
        fundingOrderRepository.save(refundInitiated);

        try {
            paymentGateway.refund(order.stripePaymentIntentId(), amount);
        } catch (FundingFailedException e) {
            var refundFailed = refundInitiated.toBuilder().status(FundingStatus.REFUND_FAILED).build();
            fundingOrderRepository.save(refundFailed);
            log.error(
                    "Stripe refund failed fundingId={} paymentIntentId={}",
                    fundingId, order.stripePaymentIntentId(), e);
            throw RefundFailedException.stripeRefundFailed(order.stripePaymentIntentId(), e);
        }

        var decrementedWallet = wallet.toBuilder()
                .availableBalance(wallet.availableBalance().subtract(amount))
                .totalBalance(wallet.totalBalance().subtract(amount))
                .build();
        walletRepository.save(decrementedWallet);

        var refunded = refundInitiated.toBuilder().status(FundingStatus.REFUNDED).build();
        var saved = fundingOrderRepository.save(refunded);

        log.info(
                "Refund completed fundingId={} walletId={} amount={} newAvailable={}",
                fundingId, order.walletId(), amount, decrementedWallet.availableBalance());

        return saved;
    }

    private void assertSufficientBalance(Wallet wallet, BigDecimal amount) {
        var onChainBalance = treasuryService.getUsdcBalance(wallet.solanaAddress());
        if (onChainBalance.compareTo(amount) < 0) {
            throw InsufficientBalanceForRefundException.forAmount(amount, onChainBalance);
        }
        if (wallet.availableBalance().compareTo(amount) < 0) {
            throw InsufficientBalanceForRefundException.forAmount(amount, wallet.availableBalance());
        }
    }
}
