package com.stablepay.domain.auth.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public static UserNotFoundException byId(UUID userId) {
        return new UserNotFoundException("SP-0037: User not found: " + userId);
    }

    private UserNotFoundException(String message) {
        super(message);
    }
}
