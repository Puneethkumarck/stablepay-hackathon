package com.stablepay.infrastructure.disbursement;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.stablepay.domain.common.PiiMasking;
import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "stablepay.disbursement.provider",
        havingValue = "logging",
        matchIfMissing = true)
public class LoggingDisbursementAdapter implements FiatDisbursementProvider {

    private static final String SIMULATED_STATUS = "SIMULATED";

    @Override
    public DisbursementResult disburse(
            String upiId,
            BigDecimal amountUsdc,
            BigDecimal amountInr,
            String remittanceId) {
        log.info(
                "Simulating INR disbursement: {} USDC ({} INR) to UPI {} for remittance {}",
                amountUsdc,
                amountInr,
                PiiMasking.maskUpi(upiId),
                remittanceId);
        var result = DisbursementResult.builder()
                .providerId("log_" + UUID.randomUUID())
                .providerStatus(SIMULATED_STATUS)
                .build();
        log.info(
                "INR disbursement simulated successfully for remittance {} providerId={}",
                remittanceId,
                result.providerId());
        return result;
    }
}
