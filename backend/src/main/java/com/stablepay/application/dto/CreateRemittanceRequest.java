package com.stablepay.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Builder;

@Builder(toBuilder = true)
public record CreateRemittanceRequest(
    @NotBlank String recipientPhone,
    @NotNull @Positive BigDecimal amountUsdc,
    @Size(max = 100) String recipientName
) {}
