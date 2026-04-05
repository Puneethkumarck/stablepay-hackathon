package com.stablepay.domain.remittance.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RemittanceStatusTest {

    @Test
    void shouldAllowInitiatedToEscrowed() {
        assertThat(RemittanceStatus.INITIATED.canTransitionTo(RemittanceStatus.ESCROWED)).isTrue();
    }

    @Test
    void shouldAllowInitiatedToCancelled() {
        assertThat(RemittanceStatus.INITIATED.canTransitionTo(RemittanceStatus.CANCELLED)).isTrue();
    }

    @Test
    void shouldAllowEscrowedToClaimed() {
        assertThat(RemittanceStatus.ESCROWED.canTransitionTo(RemittanceStatus.CLAIMED)).isTrue();
    }

    @Test
    void shouldAllowEscrowedToRefunded() {
        assertThat(RemittanceStatus.ESCROWED.canTransitionTo(RemittanceStatus.REFUNDED)).isTrue();
    }

    @Test
    void shouldAllowEscrowedToCancelled() {
        assertThat(RemittanceStatus.ESCROWED.canTransitionTo(RemittanceStatus.CANCELLED)).isTrue();
    }

    @Test
    void shouldAllowClaimedToDelivered() {
        assertThat(RemittanceStatus.CLAIMED.canTransitionTo(RemittanceStatus.DELIVERED)).isTrue();
    }

    @Test
    void shouldRejectDeliveredToInitiated() {
        assertThat(RemittanceStatus.DELIVERED.canTransitionTo(RemittanceStatus.INITIATED)).isFalse();
    }

    @Test
    void shouldRejectInitiatedToDelivered() {
        assertThat(RemittanceStatus.INITIATED.canTransitionTo(RemittanceStatus.DELIVERED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RemittanceStatus.class)
    void shouldRejectAllTransitionsFromDelivered(RemittanceStatus target) {
        assertThat(RemittanceStatus.DELIVERED.canTransitionTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RemittanceStatus.class)
    void shouldRejectAllTransitionsFromRefunded(RemittanceStatus target) {
        assertThat(RemittanceStatus.REFUNDED.canTransitionTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RemittanceStatus.class)
    void shouldRejectAllTransitionsFromCancelled(RemittanceStatus target) {
        assertThat(RemittanceStatus.CANCELLED.canTransitionTo(target)).isFalse();
    }
}
