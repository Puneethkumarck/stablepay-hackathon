package com.stablepay.domain.claim.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.stablepay.testutil.ClaimTokenFixtures;

class ClaimTokenTest {

    @Test
    void shouldCreateClaimTokenWithExpiry() {
        // given / when
        var claimToken = ClaimTokenFixtures.claimTokenBuilder().build();

        // then
        var expected = ClaimTokenFixtures.claimTokenBuilder().build();

        assertThat(claimToken)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldGenerateUrlSafeToken() {
        // given
        var token = UUID.randomUUID().toString();

        // when
        var claimToken = ClaimTokenFixtures.claimTokenBuilder()
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
                .token(ClaimTokenFixtures.SOME_TOKEN)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("remittanceId");
    }

    @Test
    void shouldThrowWhenTokenIsNull() {
        // when / then
        assertThatThrownBy(() -> ClaimToken.builder()
                .remittanceId(ClaimTokenFixtures.SOME_REMITTANCE_ID)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("token");
    }
}
