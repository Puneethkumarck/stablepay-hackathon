package com.stablepay.domain.port.outbound;

import java.util.Optional;

import com.stablepay.domain.model.Wallet;

public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(Long id);
    Optional<Wallet> findByUserId(String userId);
}
