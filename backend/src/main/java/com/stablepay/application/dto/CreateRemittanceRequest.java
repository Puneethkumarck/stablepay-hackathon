package com.stablepay.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Builder;

@Builder(toBuilder = true)
public record CreateRemittanceRequest(
    @NotNull UUID senderId,
    @NotBlank String recipientPhone,
    @NotNull @Positive BigDecimal amountUsdc
) {}
