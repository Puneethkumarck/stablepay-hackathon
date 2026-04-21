package com.stablepay.domain.funding.handler;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetFundingOrderHandler {

    private final FundingOrderRepository fundingOrderRepository;
    private final WalletRepository walletRepository;

    public FundingOrder handle(UUID fundingId, UUID authenticatedUserId) {
        var order = fundingOrderRepository.findByFundingId(fundingId)
                .orElseThrow(() -> FundingOrderNotFoundException.byFundingId(fundingId));

        var wallet = walletRepository.findById(order.walletId())
                .orElseThrow(() -> WalletNotFoundException.byId(order.walletId()));

        if (!wallet.userId().equals(authenticatedUserId)) {
            throw FundingOrderNotFoundException.byFundingId(fundingId);
        }

        return order;
    }
}
