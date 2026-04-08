package com.stablepay.domain.remittance.port;

import java.math.BigDecimal;

public interface FiatDisbursementProvider {
    void disburse(String upiId, BigDecimal amountUsdc, String remittanceId);
}
