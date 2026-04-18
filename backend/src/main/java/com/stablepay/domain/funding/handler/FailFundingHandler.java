package com.stablepay.domain.funding.handler;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FailFundingHandler {

    private static final Set<FundingStatus> TERMINAL_STATUSES = Set.of(
            FundingStatus.FUNDED,
            FundingStatus.FAILED,
            FundingStatus.REFUND_INITIATED,
            FundingStatus.REFUNDED,
            FundingStatus.REFUND_FAILED);

    private final FundingOrderRepository fundingOrderRepository;

    public void handle(UUID fundingId) {
        if (fundingId == null) {
            log.warn("Ignoring payment_failed with null fundingId");
            return;
        }
        var orderOpt = fundingOrderRepository.findByFundingId(fundingId);
        if (orderOpt.isEmpty()) {
            log.warn("Funding order not found on payment_failed fundingId={}", fundingId);
            return;
        }
        var order = orderOpt.get();
        if (TERMINAL_STATUSES.contains(order.status())) {
            log.debug("Ignoring payment_failed for fundingId={} already in status={}",
                    fundingId, order.status());
            return;
        }
        if (order.status() != FundingStatus.PAYMENT_CONFIRMED) {
            log.warn("Unexpected status on payment_failed fundingId={} status={}",
                    fundingId, order.status());
            return;
        }

        var failed = order.toBuilder().status(FundingStatus.FAILED).build();
        var persisted = fundingOrderRepository.save(failed);
        log.info("Funding order marked failed fundingId={} walletId={}",
                persisted.fundingId(), persisted.walletId());
    }
}
