package com.stablepay.domain.port.outbound;

import java.math.BigDecimal;

public interface TreasuryService {
    void transferFromTreasury(String destinationAddress, BigDecimal amount);
}
