package com.stablepay.domain.claim.handler;

import static com.stablepay.testutil.ClaimTokenFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_TOKEN;
import static com.stablepay.testutil.ClaimTokenFixtures.claimTokenBuilder;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.claim.model.ClaimDetails;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.port.RemittanceRepository;

@ExtendWith(MockitoExtension.class)
class GetClaimQueryHandlerTest {

    @Mock
    private ClaimTokenRepository claimTokenRepository;

    @Mock
    private RemittanceRepository remittanceRepository;

    @InjectMocks
    private GetClaimQueryHandler getClaimQueryHandler;

    @Test
    void shouldReturnClaimDetailsForValidToken() {
        // given
        var claimToken = claimTokenBuilder().build();
        var remittance = remittanceBuilder().build();

        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID)).willReturn(Optional.of(remittance));

        // when
        var result = getClaimQueryHandler.handle(SOME_TOKEN);

        // then
        var expected = ClaimDetails.builder()
                .claimToken(claimToken)
                .remittance(remittance)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        // given
        given(claimTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> getClaimQueryHandler.handle("unknown-token"))
                .isInstanceOf(ClaimTokenNotFoundException.class)
                .hasMessageContaining("SP-0011");
    }

    @Test
    void shouldThrowWhenRemittanceNotFound() {
        // given
        var claimToken = claimTokenBuilder().build();
        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> getClaimQueryHandler.handle(SOME_TOKEN))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010");
    }
}
