package com.stablepay.domain.wallet.port;

import java.math.BigDecimal;

public interface TreasuryService {
    void transferFromTreasury(String destinationAddress, BigDecimal amount);

    BigDecimal getBalance();
}
