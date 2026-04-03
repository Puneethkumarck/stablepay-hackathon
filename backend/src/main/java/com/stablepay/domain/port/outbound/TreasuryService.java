package com.stablepay.domain.port.outbound;

import java.math.BigDecimal;

public interface TreasuryService {
    BigDecimal getBalance();
    void transferFromTreasury(String destinationAddress, BigDecimal amount);
}
