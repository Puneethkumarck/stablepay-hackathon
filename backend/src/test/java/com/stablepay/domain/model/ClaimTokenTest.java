package com.stablepay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ClaimTokenTest {

    @Test
    void shouldCreateClaimTokenWithExpiry() {
        // given
        var token = UUID.randomUUID().toString();
        var remittanceId = UUID.randomUUID();
        var now = Instant.now();
        var expiresAt = now.plus(48, ChronoUnit.HOURS);

        // when
        var claimToken = ClaimToken.builder()
                .remittanceId(remittanceId)
                .token(token)
                .claimed(false)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        // then
        var expected = ClaimToken.builder()
                .remittanceId(remittanceId)
                .token(token)
                .claimed(false)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        assertThat(claimToken)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldGenerateUrlSafeToken() {
        // given
        var token = UUID.randomUUID().toString();

        // when
        var claimToken = ClaimToken.builder()
                .remittanceId(UUID.randomUUID())
                .token(token)
                .build();

        // then
        assertThat(claimToken.token())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void shouldThrowWhenRemittanceIdIsNull() {
        // when / then
        assertThatThrownBy(() -> ClaimToken.builder()
                .token("some-token")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("remittanceId");
    }

    @Test
    void shouldThrowWhenTokenIsNull() {
        // when / then
        assertThatThrownBy(() -> ClaimToken.builder()
                .remittanceId(UUID.randomUUID())
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("token");
    }
}
