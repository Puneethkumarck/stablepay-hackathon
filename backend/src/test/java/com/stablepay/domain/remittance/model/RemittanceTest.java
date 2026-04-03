package com.stablepay.domain.remittance.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.stablepay.testutil.RemittanceFixtures;

class RemittanceTest {

    @Test
    void shouldCreateRemittanceWithInitiatedStatus() {
        // given / when
        var remittance = RemittanceFixtures.remittanceBuilder().build();

        // then
        var expected = RemittanceFixtures.remittanceBuilder().build();

        assertThat(remittance)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldTransitionFromInitiatedToEscrowed() {
        // given
        var remittance = RemittanceFixtures.remittanceBuilder().build();

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
        var remittance = RemittanceFixtures.remittanceBuilder()
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
        var remittance = RemittanceFixtures.remittanceBuilder()
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
                .senderId(RemittanceFixtures.SOME_SENDER_ID)
                .recipientPhone(RemittanceFixtures.SOME_RECIPIENT_PHONE)
                .amountUsdc(RemittanceFixtures.SOME_AMOUNT_USDC)
                .status(RemittanceStatus.INITIATED)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("remittanceId");
    }

    @Test
    void shouldThrowWhenStatusIsNull() {
        // when / then
        assertThatThrownBy(() -> Remittance.builder()
                .remittanceId(RemittanceFixtures.SOME_REMITTANCE_ID)
                .senderId(RemittanceFixtures.SOME_SENDER_ID)
                .recipientPhone(RemittanceFixtures.SOME_RECIPIENT_PHONE)
                .amountUsdc(RemittanceFixtures.SOME_AMOUNT_USDC)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }
}
