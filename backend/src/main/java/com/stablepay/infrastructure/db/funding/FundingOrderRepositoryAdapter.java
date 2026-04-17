package com.stablepay.infrastructure.db.funding;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class FundingOrderRepositoryAdapter implements FundingOrderRepository {

    private final FundingOrderJpaRepository jpaRepository;
    private final FundingOrderEntityMapper mapper;

    @Override
    public FundingOrder save(FundingOrder order) {
        var entity = mapper.toEntity(order);
        var now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FundingOrder> findByFundingId(UUID fundingId) {
        return jpaRepository.findByFundingId(fundingId).map(mapper::toDomain);
    }

    @Override
    public Optional<FundingOrder> findByStripePaymentIntentId(String paymentIntentId) {
        return jpaRepository.findByStripePaymentIntentId(paymentIntentId).map(mapper::toDomain);
    }

    @Override
    public List<FundingOrder> findByWalletIdAndStatusIn(Long walletId, List<FundingStatus> statuses) {
        return jpaRepository.findByWalletIdAndStatusIn(walletId, statuses).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
