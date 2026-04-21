package com.stablepay.domain.auth.exception;

import java.util.UUID;

public class RefreshTokenExpiredException extends RuntimeException {

    public static RefreshTokenExpiredException forToken(UUID tokenId) {
        return new RefreshTokenExpiredException("SP-0036: Refresh token expired: " + tokenId);
    }

    private RefreshTokenExpiredException(String message) {
        super(message);
    }
}
