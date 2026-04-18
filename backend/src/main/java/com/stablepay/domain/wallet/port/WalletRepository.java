package com.stablepay.domain.wallet.port;

import java.util.Optional;

import com.stablepay.domain.wallet.model.Wallet;

public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(Long id);
    Optional<Wallet> findByIdForUpdate(Long id);
    Optional<Wallet> findByUserId(String userId);
    Optional<Wallet> findBySolanaAddress(String solanaAddress);
    boolean existsById(Long id);
}
