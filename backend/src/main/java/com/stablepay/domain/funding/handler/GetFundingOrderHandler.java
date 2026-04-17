package com.stablepay.domain.funding.handler;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.port.FundingOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetFundingOrderHandler {

    private final FundingOrderRepository fundingOrderRepository;

    public FundingOrder handle(UUID fundingId) {
        return fundingOrderRepository.findByFundingId(fundingId)
                .orElseThrow(() -> FundingOrderNotFoundException.byFundingId(fundingId));
    }
}
