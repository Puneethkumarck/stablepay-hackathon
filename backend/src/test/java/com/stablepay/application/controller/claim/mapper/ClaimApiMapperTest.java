package com.stablepay.application.controller.claim.mapper;

import static com.stablepay.testutil.AuthFixtures.SOME_SENDER_DISPLAY_NAME;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_EXPIRES_AT;
import static com.stablepay.testutil.ClaimTokenFixtures.claimTokenBuilder;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.application.dto.ClaimResponse;
import com.stablepay.domain.claim.model.ClaimDetails;
import com.stablepay.domain.remittance.model.RemittanceStatus;

class ClaimApiMapperTest {

    private final ClaimApiMapper mapper = new ClaimApiMapperImpl();

    @Test
    void shouldMapClaimDetailsToResponse() {
        // given
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .build();
        var claimToken = claimTokenBuilder()
                .claimed(false)
                .build();
        var claimDetails = ClaimDetails.builder()
                .claimToken(claimToken)
                .remittance(remittance)
                .senderDisplayName(SOME_SENDER_DISPLAY_NAME)
                .build();

        // when
        var response = mapper.toResponse(claimDetails);

        // then
        var expected = ClaimResponse.builder()
                .remittanceId(remittance.remittanceId())
                .senderDisplayName(SOME_SENDER_DISPLAY_NAME)
                .amountUsdc(remittance.amountUsdc())
                .amountInr(remittance.amountInr())
                .fxRate(remittance.fxRate())
                .status(RemittanceStatus.ESCROWED)
                .claimed(false)
                .expiresAt(SOME_EXPIRES_AT)
                .build();

        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
