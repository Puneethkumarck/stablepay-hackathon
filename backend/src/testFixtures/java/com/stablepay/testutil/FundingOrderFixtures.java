package com.stablepay.testutil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;

public final class FundingOrderFixtures {

    private FundingOrderFixtures() {}

    public static final Long SOME_FUNDING_ORDER_DB_ID = 1L;
    public static final UUID SOME_FUNDING_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    public static final Long SOME_WALLET_ID = 4L;
    public static final BigDecimal SOME_AMOUNT_USDC = new BigDecimal("25.00");
    public static final String SOME_STRIPE_PAYMENT_INTENT_ID = "pi_3MnTest0123456789";
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-04-16T15:00:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-04-16T15:00:00Z");

    public static FundingOrder.FundingOrderBuilder fundingOrderBuilder() {
        return FundingOrder.builder()
                .id(SOME_FUNDING_ORDER_DB_ID)
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT);
    }
}
