package com.stablepay.domain.wallet.port;

import java.math.BigDecimal;

public interface TreasuryService {

    String transferUsdc(String destinationAddress, BigDecimal amountUsdc);

    String transferSol(String destinationAddress, long lamports);

    BigDecimal getSolBalance(String address);

    BigDecimal getUsdcBalance(String address);

    BigDecimal getTreasuryUsdcBalance();

    void createAtaIfNeeded(String ownerAddress);
}
