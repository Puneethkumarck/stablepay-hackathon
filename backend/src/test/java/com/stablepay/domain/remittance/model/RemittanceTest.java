package com.stablepay.domain.remittance.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RemittanceTest {

    @Test
    void shouldCreateRemittanceWithInitiatedStatus() {
        // given
        var remittanceId = UUID.randomUUID();
        var now = Instant.now();

        // when
        var remittance = Remittance.builder()
                .remittanceId(remittanceId)
                .senderId("user-1")
                .recipientPhone("+919876543210")
                .amountUsdc(BigDecimal.valueOf(100))
                .amountInr(BigDecimal.valueOf(8450))
                .fxRate(BigDecimal.valueOf(84.50))
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .createdAt(now)
                .build();

        // then
        var expected = Remittance.builder()
                .remittanceId(remittanceId)
                .senderId("user-1")
                .recipientPhone("+919876543210")
                .amountUsdc(BigDecimal.valueOf(100))
                .amountInr(BigDecimal.valueOf(8450))
                .fxRate(BigDecimal.valueOf(84.50))
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .createdAt(now)
                .build();

        assertThat(remittance)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldTransitionFromInitiatedToEscrowed() {
        // given
        var remittance = Remittance.builder()
                .remittanceId(UUID.randomUUID())
                .senderId("user-1")
                .recipientPhone("+919876543210")
                .amountUsdc(BigDecimal.valueOf(100))
                .status(RemittanceStatus.INITIATED)
                .build();

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
        var remittance = Remittance.builder()
                .remittanceId(UUID.randomUUID())
                .senderId("user-1")
                .recipientPhone("+919876543210")
                .amountUsdc(BigDecimal.valueOf(100))
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
        var remittance = Remittance.builder()
                .remittanceId(UUID.randomUUID())
                .senderId("user-1")
                .recipientPhone("+919876543210")
                .amountUsdc(BigDecimal.valueOf(100))
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
                .senderId("user-1")
                .recipientPhone("+919876543210")
                .amountUsdc(BigDecimal.valueOf(100))
                .status(RemittanceStatus.INITIATED)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("remittanceId");
    }

    @Test
    void shouldThrowWhenStatusIsNull() {
        // when / then
        assertThatThrownBy(() -> Remittance.builder()
                .remittanceId(UUID.randomUUID())
                .senderId("user-1")
                .recipientPhone("+919876543210")
                .amountUsdc(BigDecimal.valueOf(100))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }
}
