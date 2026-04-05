package com.stablepay.testutil;

import java.time.Instant;
import java.util.UUID;

import com.stablepay.domain.claim.model.ClaimToken;

public final class ClaimTokenFixtures {

    private ClaimTokenFixtures() {}

    public static final Long SOME_CLAIM_TOKEN_ID = 1L;
    public static final UUID SOME_REMITTANCE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final String SOME_TOKEN = "claim-token-abc-123";
    public static final String SOME_UPI_ID = "recipient@upi";
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");
    public static final Instant SOME_EXPIRES_AT = Instant.parse("2026-04-05T10:00:00Z");
    public static final Instant SOME_EXPIRED_AT = Instant.parse("2026-04-01T10:00:00Z");

    public static ClaimToken.ClaimTokenBuilder claimTokenBuilder() {
        return ClaimToken.builder()
                .id(SOME_CLAIM_TOKEN_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .token(SOME_TOKEN)
                .claimed(false)
                .createdAt(SOME_CREATED_AT)
                .expiresAt(SOME_EXPIRES_AT);
    }
}
