package com.stablepay.testutil;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import com.stablepay.infrastructure.temporal.ClaimSignal;
import com.stablepay.infrastructure.temporal.RemittanceWorkflowRequest;
import com.stablepay.infrastructure.temporal.RemittanceWorkflowResult;
import com.stablepay.infrastructure.temporal.RemittanceWorkflowStatus;

public final class WorkflowFixtures {

    private WorkflowFixtures() {}

    public static final UUID SOME_REMITTANCE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final String SOME_SENDER_ADDRESS = "SoLaNa1234567890AbCdEfGhIjKlMnOpQrStUvWx";
    public static final String SOME_RECIPIENT_PHONE = "+919876543210";
    public static final BigDecimal SOME_AMOUNT_USDC = BigDecimal.valueOf(100);
    public static final String SOME_CLAIM_TOKEN = "claim-token-abc-123";
    public static final String SOME_ESCROW_PDA = "EsCrOwPdA1234567890AbCdEfGhIjKlMnOpQrStUv";
    public static final String SOME_TX_SIGNATURE = "5wHu1qwD7xQFhkP3L3jE9YmN9mQfTkRzHcGzYqNz";
    public static final String SOME_UPI_ID = "recipient@upi";
    public static final String SOME_DESTINATION_ADDRESS = "DsT1NaTi0nAdDr3sS9876543210AbCdEfGhIjKl";
    public static final String SOME_CLAIM_BASE_URL = "https://claim.stablepay.app/";
    public static final Duration SOME_CLAIM_EXPIRY_TIMEOUT = Duration.ofHours(48);

    public static RemittanceWorkflowRequest.RemittanceWorkflowRequestBuilder workflowRequestBuilder() {
        return RemittanceWorkflowRequest.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .senderAddress(SOME_SENDER_ADDRESS)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .claimToken(SOME_CLAIM_TOKEN)
                .claimBaseUrl(SOME_CLAIM_BASE_URL)
                .claimExpiryTimeout(SOME_CLAIM_EXPIRY_TIMEOUT);
    }

    public static RemittanceWorkflowResult.RemittanceWorkflowResultBuilder workflowResultBuilder() {
        return RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus("DELIVERED")
                .escrowPda(SOME_ESCROW_PDA)
                .txSignature(SOME_TX_SIGNATURE);
    }

    public static RemittanceWorkflowStatus.RemittanceWorkflowStatusBuilder workflowStatusBuilder() {
        return RemittanceWorkflowStatus.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .currentStatus("INITIATED")
                .smsNotificationFailed(false);
    }

    public static ClaimSignal.ClaimSignalBuilder claimSignalBuilder() {
        return ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS);
    }
}
