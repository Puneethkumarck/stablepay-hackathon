package com.stablepay.domain.remittance.model;

import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RemittanceTest {

    @Test
    void shouldCreateRemittanceWithInitiatedStatus() {
        // given / when
        var remittance = remittanceBuilder().build();

        // then
        var expected = remittanceBuilder().build();

        assertThat(remittance)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldTransitionFromInitiatedToEscrowed() {
        // given
        var remittance = remittanceBuilder().build();

        // when
        var escrowed = remittance.toBuilder()
                .status(RemittanceStatus.ESCROWED)
                .escrowPda("EscrowPDA123")
                .build();

        // then
        var expected = remittance.toBuilder()
                .status(RemittanceStatus.ESCROWED)
                .escrowPda("EscrowPDA123")
                .build();

        assertThat(escrowed)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldTransitionFromEscrowedToClaimed() {
        // given
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .build();

        // when
        var claimed = remittance.toBuilder()
                .status(RemittanceStatus.CLAIMED)
                .build();

        // then
        var expected = remittance.toBuilder()
                .status(RemittanceStatus.CLAIMED)
                .build();

        assertThat(claimed)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldTransitionFromClaimedToDelivered() {
        // given
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.CLAIMED)
                .build();

        // when
        var delivered = remittance.toBuilder()
                .status(RemittanceStatus.DELIVERED)
                .build();

        // then
        var expected = remittance.toBuilder()
                .status(RemittanceStatus.DELIVERED)
                .build();

        assertThat(delivered)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenRemittanceIdIsNull() {
        // when / then
        assertThatThrownBy(() -> Remittance.builder()
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(RemittanceStatus.INITIATED)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("remittanceId");
    }

    @Test
    void shouldThrowWhenStatusIsNull() {
        // when / then
        assertThatThrownBy(() -> Remittance.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }
}
