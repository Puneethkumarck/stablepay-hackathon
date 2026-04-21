package com.stablepay.application.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Builder;

@Builder(toBuilder = true)
public record SocialLoginRequest(
    @NotBlank String provider,
    @NotBlank String idToken
) {}
