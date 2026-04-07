package com.stablepay.domain.remittance.port;

public interface FiatDisbursementProvider {
    void disburse(String upiId, String amountInr, String remittanceId);
}
