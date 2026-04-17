package com.stablepay.domain.funding.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;

public interface FundingOrderRepository {
    FundingOrder save(FundingOrder order);
    Optional<FundingOrder> findByFundingId(UUID fundingId);
    Optional<FundingOrder> findByStripePaymentIntentId(String paymentIntentId);
    List<FundingOrder> findByWalletIdAndStatusIn(Long walletId, List<FundingStatus> statuses);
}
