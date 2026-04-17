package com.stablepay.infrastructure.db.funding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stablepay.domain.funding.model.FundingStatus;

interface FundingOrderJpaRepository extends JpaRepository<FundingOrderEntity, Long> {
    Optional<FundingOrderEntity> findByFundingId(UUID fundingId);
    Optional<FundingOrderEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<FundingOrderEntity> findByWalletIdAndStatusIn(Long walletId, List<FundingStatus> statuses);
}
