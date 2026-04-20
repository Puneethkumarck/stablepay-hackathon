package com.stablepay.domain.remittance.model;

import java.util.Map;
import java.util.Set;

public enum RemittanceStatus {
    INITIATED,
    ESCROWED,
    CLAIMED,
    DELIVERED,
    DISBURSEMENT_FAILED,
    DEPOSIT_FAILED,
    CLAIM_FAILED,
    REFUND_FAILED,
    REFUNDED,
    CANCELLED;

    private static final Map<RemittanceStatus, Set<RemittanceStatus>> VALID_TRANSITIONS = Map.of(
            INITIATED, Set.of(ESCROWED, CANCELLED, DEPOSIT_FAILED),
            ESCROWED, Set.of(CLAIMED, REFUNDED, CANCELLED, CLAIM_FAILED, REFUND_FAILED),
            CLAIMED, Set.of(DELIVERED, DISBURSEMENT_FAILED)
    );

    public boolean canTransitionTo(RemittanceStatus target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
