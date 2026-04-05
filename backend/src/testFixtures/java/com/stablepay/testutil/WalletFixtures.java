package com.stablepay.testutil;

import java.math.BigDecimal;
import java.time.Instant;

import com.stablepay.domain.wallet.model.Wallet;

public final class WalletFixtures {

    private WalletFixtures() {}

    public static final Long SOME_WALLET_ID = 1L;
    public static final String SOME_USER_ID = "user-42";
    public static final String SOME_SOLANA_ADDRESS = "SoLaNa1234567890AbCdEfGhIjKlMnOpQrStUvWx";
    public static final byte[] SOME_PUBLIC_KEY = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    public static final byte[] SOME_KEY_SHARE_DATA = new byte[]{10, 20, 30, 40, 50};
    public static final BigDecimal SOME_BALANCE = BigDecimal.valueOf(100);
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-04-03T10:00:00Z");

    public static Wallet.WalletBuilder walletBuilder() {
        return Wallet.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(SOME_BALANCE)
                .totalBalance(SOME_BALANCE)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT);
    }
}
