package com.stablepay.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stablepay.domain.funding.model.FundingStatus;

import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FundingOrderResponse(
    UUID fundingId,
    Long walletId,
    BigDecimal amountUsdc,
    FundingStatus status,
    String stripePaymentIntentId,
    String stripeClientSecret,
    Instant createdAt
) {}
