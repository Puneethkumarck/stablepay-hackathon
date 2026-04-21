package com.stablepay.domain.wallet.handler;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetWalletQueryHandler {

    private final WalletRepository walletRepository;

    public Wallet handle(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> WalletNotFoundException.byUserId(userId));
    }
}
