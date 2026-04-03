package com.stablepay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ClaimTokenTest {

    @Test
    void shouldGenerateUrlSafeToken() {
        // given
        var token = UUID.randomUUID().toString();

        // when
        var claimToken = ClaimToken.builder()
                .remittanceId(UUID.randomUUID())
                .token(token)
                .claimed(false)
                .createdAt(Instant.now())
                .build();

        // then
        assertThat(claimToken.token())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(claimToken.claimed()).isFalse();
    }
}
