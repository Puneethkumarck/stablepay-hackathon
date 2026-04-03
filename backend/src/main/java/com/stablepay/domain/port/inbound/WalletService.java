package com.stablepay.domain.port.inbound;

import java.math.BigDecimal;

import com.stablepay.domain.model.Wallet;

public interface WalletService {
    Wallet create(String userId);
    Wallet fund(Long walletId, BigDecimal amount);
}
