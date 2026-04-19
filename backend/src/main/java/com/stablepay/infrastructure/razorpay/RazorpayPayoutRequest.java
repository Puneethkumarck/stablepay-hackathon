package com.stablepay.infrastructure.razorpay;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder(toBuilder = true)
record RazorpayPayoutRequest(
        @JsonProperty("account_number") String accountNumber,
        @JsonProperty("fund_account_id") String fundAccountId,
        long amount,
        String currency,
        String mode,
        String purpose,
        @JsonProperty("queue_if_low_balance") boolean queueIfLowBalance,
        @JsonProperty("reference_id") String referenceId,
        String narration) {

    static RazorpayPayoutRequest forPayout(
            String accountNumber, String fundAccountId, long paise, String remittanceId) {
        return RazorpayPayoutRequest.builder()
                .accountNumber(accountNumber)
                .fundAccountId(fundAccountId)
                .amount(paise)
                .currency("INR")
                .mode("UPI")
                .purpose("payout")
                .queueIfLowBalance(false)
                .referenceId(remittanceId)
                .narration("StablePay remittance")
                .build();
    }
}
