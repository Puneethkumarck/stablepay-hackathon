package com.stablepay.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;

@Builder(toBuilder = true)
public record CreateWalletRequest(
    @NotNull UUID userId
) {}
