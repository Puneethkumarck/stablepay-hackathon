package com.stablepay.infrastructure.transak;

import java.math.BigDecimal;

import com.stablepay.domain.common.PiiMasking;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingDisbursementAdapter implements FiatDisbursementProvider {

    @Override
    public void disburse(String upiId, BigDecimal amountUsdc, String remittanceId) {
        log.info("Simulating INR disbursement: {} USDC to UPI {} for remittance {}",
                amountUsdc, PiiMasking.maskUpi(upiId), remittanceId);
        log.info("INR disbursement simulated successfully for remittance {}", remittanceId);
    }
}
