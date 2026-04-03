package com.stablepay.testutil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;

public final class RemittanceFixtures {

    private RemittanceFixtures() {}

    public static final UUID SOME_REMITTANCE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final String SOME_SENDER_ID = "user-42";
    public static final String SOME_RECIPIENT_PHONE = "+919876543210";
    public static final BigDecimal SOME_AMOUNT_USDC = BigDecimal.valueOf(100);
    public static final BigDecimal SOME_AMOUNT_INR = BigDecimal.valueOf(8450);
    public static final BigDecimal SOME_FX_RATE = new BigDecimal("84.50");
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");

    public static Remittance.RemittanceBuilder remittanceBuilder() {
        return Remittance.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .createdAt(SOME_CREATED_AT);
    }
}
