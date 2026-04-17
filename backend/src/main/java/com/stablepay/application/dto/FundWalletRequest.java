package com.stablepay.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;

@Builder(toBuilder = true)
public record FundWalletRequest(
    @NotNull
    @DecimalMin(value = "1.00", inclusive = true)
    @DecimalMax(value = "10000.00", inclusive = true)
    @Digits(integer = 5, fraction = 2)
    BigDecimal amount
) {}
