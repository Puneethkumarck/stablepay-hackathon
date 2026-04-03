package com.stablepay.application.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Builder;

@Builder(toBuilder = true)
public record CreateWalletRequest(
    @NotBlank String userId
) {}
