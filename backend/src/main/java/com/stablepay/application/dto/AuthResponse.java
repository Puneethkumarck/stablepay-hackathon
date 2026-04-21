package com.stablepay.application.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    UserResponse user,
    WalletResponse wallet
) {}
