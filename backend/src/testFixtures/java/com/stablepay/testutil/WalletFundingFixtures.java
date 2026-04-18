package com.stablepay.testutil;

import java.math.BigDecimal;
import java.util.UUID;

import com.stablepay.infrastructure.temporal.WalletFundingWorkflowRequest;

public final class WalletFundingFixtures {

    private WalletFundingFixtures() {}

    public static final UUID SOME_FUNDING_ID =
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    public static final Long SOME_WALLET_ID = 7L;
    public static final String SOME_SENDER_ADDRESS =
            "SeNdEr1234567890AbCdEfGhIjKlMnOpQrStUvWx";
    public static final BigDecimal SOME_AMOUNT_USDC = new BigDecimal("25.00");
    public static final BigDecimal SOME_TREASURY_BALANCE = new BigDecimal("1000.00");
    public static final BigDecimal SOME_SUFFICIENT_SOL = new BigDecimal("0.05");
    public static final BigDecimal SOME_LOW_SOL = new BigDecimal("0.001");
    public static final String SOME_TX_SIGNATURE = "FundingTxSig1234567890AbCdEfGhIjKlMnOpQr";

    public static WalletFundingWorkflowRequest.WalletFundingWorkflowRequestBuilder requestBuilder() {
        return WalletFundingWorkflowRequest.builder()
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .senderSolanaAddress(SOME_SENDER_ADDRESS)
                .amountUsdc(SOME_AMOUNT_USDC);
    }
}
