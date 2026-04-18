package com.stablepay.domain.funding.handler;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinalizeFundingHandler {

    private final WalletRepository walletRepository;
    private final FundingOrderRepository fundingOrderRepository;

    @Transactional
    public void handle(UUID fundingId, Long walletId, BigDecimal amountUsdc) {
        requireNonNull(fundingId, "fundingId cannot be null");
        requireNonNull(walletId, "walletId cannot be null");
        requireNonNull(amountUsdc, "amountUsdc cannot be null");

        var wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> WalletNotFoundException.byId(walletId));
        var order = fundingOrderRepository.findByFundingId(fundingId)
                .orElseThrow(() -> FundingOrderNotFoundException.byFundingId(fundingId));

        if (order.status() == FundingStatus.FUNDED) {
            log.info("finalizeFunding no-op: fundingId={} already FUNDED", fundingId);
            return;
        }

        var creditedWallet = wallet.toBuilder()
                .availableBalance(wallet.availableBalance().add(amountUsdc))
                .totalBalance(wallet.totalBalance().add(amountUsdc))
                .build();
        walletRepository.save(creditedWallet);

        var fundedOrder = order.toBuilder().status(FundingStatus.FUNDED).build();
        fundingOrderRepository.save(fundedOrder);

        log.info(
                "finalizeFunding committed fundingId={} walletId={} amount={} newAvailable={}",
                fundingId, walletId, amountUsdc, creditedWallet.availableBalance());
    }
}
