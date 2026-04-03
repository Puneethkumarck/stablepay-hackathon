package com.stablepay.domain.wallet.handler;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.TreasuryService;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FundWalletHandler {

    private final WalletRepository walletRepository;
    private final TreasuryService treasuryService;

    public Wallet handle(Long walletId, BigDecimal amount) {
        var wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> WalletNotFoundException.byId(walletId));

        var treasuryBalance = treasuryService.getBalance();
        if (treasuryBalance.compareTo(amount) < 0) {
            throw TreasuryDepletedException.insufficientTreasury(amount, treasuryBalance);
        }

        treasuryService.transferFromTreasury(wallet.solanaAddress(), amount);
        log.info("Funded wallet id={} with amount={}", walletId, amount);

        var funded = wallet.toBuilder()
                .availableBalance(wallet.availableBalance().add(amount))
                .totalBalance(wallet.totalBalance().add(amount))
                .build();

        return walletRepository.save(funded);
    }
}
