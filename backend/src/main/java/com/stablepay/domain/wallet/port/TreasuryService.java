package com.stablepay.domain.wallet.port;

import java.math.BigDecimal;

public interface TreasuryService {

    void transferUsdc(String destinationAddress, BigDecimal amountUsdc);

    void transferSol(String destinationAddress, long lamports);

    BigDecimal getSolBalance(String address);

    BigDecimal getUsdcBalance(String address);

    void createAtaIfNeeded(String ownerAddress);
}
