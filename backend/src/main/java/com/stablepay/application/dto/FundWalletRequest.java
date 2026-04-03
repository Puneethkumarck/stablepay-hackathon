package com.stablepay.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Builder;

@Builder(toBuilder = true)
public record FundWalletRequest(
    @NotNull @Positive BigDecimal amount
) {}
