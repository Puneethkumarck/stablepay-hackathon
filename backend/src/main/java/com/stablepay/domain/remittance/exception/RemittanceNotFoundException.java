package com.stablepay.domain.remittance.exception;

import java.util.UUID;

public class RemittanceNotFoundException extends RuntimeException {

    public static RemittanceNotFoundException byId(UUID remittanceId) {
        return new RemittanceNotFoundException(
                "SP-0010: Remittance not found: " + remittanceId);
    }

    private RemittanceNotFoundException(String message) {
        super(message);
    }
}
