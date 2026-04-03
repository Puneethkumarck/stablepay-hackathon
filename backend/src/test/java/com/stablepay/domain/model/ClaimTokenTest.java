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
        var remittanceId = UUID.randomUUID();
        var now = Instant.now();

        // when
        var claimToken = ClaimToken.builder()
                .remittanceId(remittanceId)
                .token(token)
                .claimed(false)
                .createdAt(now)
                .build();

        // then
        var expected = ClaimToken.builder()
                .remittanceId(remittanceId)
                .token(token)
                .claimed(false)
                .createdAt(now)
                .build();

        assertThat(claimToken)
                .usingRecursiveComparison()
                .isEqualTo(expected);
        assertThat(claimToken.token())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
