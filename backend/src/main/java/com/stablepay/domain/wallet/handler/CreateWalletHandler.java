package com.stablepay.domain.wallet.handler;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateWalletHandler {

    private final WalletRepository walletRepository;
    private final MpcWalletClient mpcWalletClient;

    public Wallet handle(String userId) {
        walletRepository.findByUserId(userId).ifPresent(existing -> {
            throw WalletAlreadyExistsException.forUserId(userId);
        });

        var solanaAddress = mpcWalletClient.generateKey();
        log.info("Generated MPC wallet for userId={}, solanaAddress={}", userId, solanaAddress);

        var wallet = Wallet.builder()
                .userId(userId)
                .solanaAddress(solanaAddress)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        return walletRepository.save(wallet);
    }
}
