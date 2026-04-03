package com.stablepay.domain.claim.model;

import static com.stablepay.testutil.ClaimTokenFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_TOKEN;
import static com.stablepay.testutil.ClaimTokenFixtures.claimTokenBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ClaimTokenTest {

    @Test
    void shouldCreateClaimTokenWithExpiry() {
        // given / when
        var claimToken = claimTokenBuilder().build();

        // then
        var expected = claimTokenBuilder().build();

        assertThat(claimToken)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldGenerateUrlSafeToken() {
        // given
        var token = UUID.randomUUID().toString();

        // when
        var claimToken = claimTokenBuilder()
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
                .token(SOME_TOKEN)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("remittanceId");
    }

    @Test
    void shouldThrowWhenTokenIsNull() {
        // when / then
        assertThatThrownBy(() -> ClaimToken.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("token");
    }
}
