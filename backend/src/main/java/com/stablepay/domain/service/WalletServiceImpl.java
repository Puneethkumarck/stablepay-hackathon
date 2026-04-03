package com.stablepay.domain.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.exception.WalletNotFoundException;
import com.stablepay.domain.model.Wallet;
import com.stablepay.domain.port.inbound.WalletService;
import com.stablepay.domain.port.outbound.MpcWalletClient;
import com.stablepay.domain.port.outbound.TreasuryService;
import com.stablepay.domain.port.outbound.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final MpcWalletClient mpcWalletClient;
    private final TreasuryService treasuryService;

    @Override
    public Wallet create(String userId) {
        log.info("Creating wallet for user: {}", userId);
        var solanaAddress = mpcWalletClient.generateKey();
        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress(solanaAddress)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();
        var saved = walletRepository.save(wallet);
        log.info("Wallet created with id: {} for user: {}", saved.id(), userId);
        return saved;
    }

    @Override
    public Wallet fund(Long walletId, BigDecimal amount) {
        log.info("Funding wallet {} with {} USDC", walletId, amount);
        var wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> WalletNotFoundException.byId(walletId));
        treasuryService.transferFromTreasury(wallet.solanaAddress(), amount);
        var funded = wallet.toBuilder()
                .availableBalance(wallet.availableBalance().add(amount))
                .totalBalance(wallet.totalBalance().add(amount))
                .build();
        var saved = walletRepository.save(funded);
        log.info("Wallet {} funded successfully. New balance: {}", walletId, saved.availableBalance());
        return saved;
    }
}
