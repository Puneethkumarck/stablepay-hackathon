package com.stablepay.testutil;

import java.math.BigDecimal;
import java.util.UUID;

public final class SolanaFixtures {

    private SolanaFixtures() {}

    public static final UUID SOME_REMITTANCE_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    public static final String SOME_PROGRAM_ID =
            "EscrowProgram111111111111111111111111111111";
    public static final String SOME_USDC_MINT =
            "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU";
    public static final String SOME_SENDER_WALLET =
            "SenderWa11et1111111111111111111111111111111";
    public static final String SOME_CLAIM_AUTHORITY =
            "C1aimAuth111111111111111111111111111111111";
    public static final String SOME_DESTINATION_TOKEN_ACCOUNT =
            "DestToken111111111111111111111111111111111";

    public static final BigDecimal SOME_AMOUNT_USDC = BigDecimal.valueOf(100);
    public static final long SOME_EXPIRY_TIMESTAMP = 1_712_300_000L;
    public static final long SOME_AMOUNT_LAMPORTS = 100_000_000L;

    public static final String SOME_TRANSACTION_SIGNATURE =
            "5VERv8NMvzbJMEkV8xnrLkEaWRtSz9CosKDYjCJjBRnbJLgp8uirBgmQpjKhoR4tjF3ZpRzrFmBV6UjKdiSZkQUW";

    public static final String SOME_BLOCKHASH =
            "GHtXQBsoZHVnNFa9YevAzFr17DJjgHXk3ycTKD5xD3Zi";

    public static final String SOME_CLAIM_AUTHORITY_PRIVATE_KEY =
            "TuncLi5MKiNXH2BG3m2wnPsMLyXkx41zwouStjE8bRmJEZ6SxxsHN21sR8RFdTQXJMBif4pBkkfX17JSPtYVZmp";

    public static final String SOME_CLAIM_AUTHORITY_PUBLIC_KEY =
            "HKqwNCgGAtg4TXmQCeMURZNpMdbz6QbvJSBVQzwQ5TC";
}
