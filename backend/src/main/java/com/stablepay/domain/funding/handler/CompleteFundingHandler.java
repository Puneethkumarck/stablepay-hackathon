package com.stablepay.domain.funding.handler;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.funding.port.FundingWorkflowStarter;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompleteFundingHandler {

    private static final Set<FundingStatus> TERMINAL_STATUSES = Set.of(
            FundingStatus.FUNDED,
            FundingStatus.FAILED,
            FundingStatus.REFUND_INITIATED,
            FundingStatus.REFUNDED,
            FundingStatus.REFUND_FAILED);

    private final FundingOrderRepository fundingOrderRepository;
    private final WalletRepository walletRepository;
    private final Optional<FundingWorkflowStarter> fundingWorkflowStarter;

    public void handle(UUID fundingId) {
        if (fundingId == null) {
            log.warn("Ignoring payment_succeeded with null fundingId");
            return;
        }
        var orderOpt = fundingOrderRepository.findByFundingId(fundingId);
        if (orderOpt.isEmpty()) {
            log.warn("Funding order not found on payment_succeeded fundingId={}", fundingId);
            return;
        }
        var order = orderOpt.get();
        if (TERMINAL_STATUSES.contains(order.status())) {
            log.debug("Ignoring payment_succeeded for fundingId={} already in status={}",
                    fundingId, order.status());
            return;
        }
        if (order.status() != FundingStatus.PAYMENT_CONFIRMED) {
            log.warn("Unexpected status on payment_succeeded fundingId={} status={}",
                    fundingId, order.status());
            return;
        }

        var walletOpt = walletRepository.findById(order.walletId());
        if (walletOpt.isEmpty()) {
            log.error("Wallet not found for funding order fundingId={} walletId={}",
                    fundingId, order.walletId());
            return;
        }
        var wallet = walletOpt.get();

        var funded = order.toBuilder().status(FundingStatus.FUNDED).build();
        FundingOrder persisted = fundingOrderRepository.save(funded);
        log.info("Funding order confirmed fundingId={} walletId={} status={}",
                persisted.fundingId(), persisted.walletId(), persisted.status());

        fundingWorkflowStarter.ifPresentOrElse(
                starter -> starter.startFundingWorkflow(
                        persisted.fundingId(),
                        persisted.walletId(),
                        wallet.solanaAddress(),
                        persisted.amountUsdc()),
                () -> log.warn(
                        "FundingWorkflowStarter not configured; skipping workflow start for fundingId={}",
                        persisted.fundingId()));
    }
}
