package com.stablepay.domain.remittance.port;

import java.math.BigDecimal;

import com.stablepay.domain.remittance.model.DisbursementResult;

public interface FiatDisbursementProvider {
    DisbursementResult disburse(
            String upiId,
            BigDecimal amountUsdc,
            BigDecimal amountInr,
            String remittanceId);
}
