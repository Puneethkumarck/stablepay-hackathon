package com.stablepay.infrastructure.transak;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.port.FiatDisbursementProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnMissingBean(FiatDisbursementProvider.class)
public class LoggingDisbursementAdapter implements FiatDisbursementProvider {

    @Override
    public void disburse(String upiId, String amountUsdc, String remittanceId) {
        log.info("Simulating INR disbursement: {} USDC to UPI {} for remittance {}",
                amountUsdc, maskUpi(upiId), remittanceId);
        log.info("INR disbursement simulated successfully for remittance {}", remittanceId);
    }

    private static String maskUpi(String upiId) {
        if (upiId == null || upiId.length() <= 4) {
            return "****";
        }
        return upiId.substring(0, 3) + "****";
    }
}
