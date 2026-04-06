package com.stablepay.domain.claim.handler;

import static com.stablepay.testutil.ClaimTokenFixtures.SOME_EXPIRED_AT;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_TOKEN;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_UPI_ID;
import static com.stablepay.testutil.ClaimTokenFixtures.claimTokenBuilder;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.claim.exception.ClaimAlreadyClaimedException;
import com.stablepay.domain.claim.exception.ClaimTokenExpiredException;
import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.claim.model.ClaimDetails;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceClaimSignaler;
import com.stablepay.domain.remittance.port.RemittanceRepository;

@ExtendWith(MockitoExtension.class)
class SubmitClaimHandlerTest {

    @Mock
    private ClaimTokenRepository claimTokenRepository;

    @Mock
    private RemittanceRepository remittanceRepository;

    @Mock
    private RemittanceClaimSignaler claimSignaler;

    private SubmitClaimHandler submitClaimHandler;

    @BeforeEach
    void setUp() {
        submitClaimHandler = new SubmitClaimHandler(
                claimTokenRepository, remittanceRepository, Optional.of(claimSignaler));
    }

    @Test
    void shouldSubmitClaimSuccessfully() {
        // given
        var claimToken = claimTokenBuilder().build();
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .build();

        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID)).willReturn(Optional.of(remittance));

        var updatedClaim = claimToken.toBuilder().claimed(true).upiId(SOME_UPI_ID).build();
        given(claimTokenRepository.save(updatedClaim)).willReturn(updatedClaim);

        // when
        var result = submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID);

        // then
        var expected = ClaimDetails.builder()
                .claimToken(updatedClaim)
                .remittance(remittance)
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(claimTokenRepository).should().save(updatedClaim);
        then(claimSignaler).should().signalClaim(SOME_REMITTANCE_ID, SOME_TOKEN, SOME_UPI_ID);
    }

    @Test
    void shouldPropagateExceptionWhenSignalerFails() {
        // given
        var claimToken = claimTokenBuilder().build();
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .build();

        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID)).willReturn(Optional.of(remittance));

        var updatedClaim = claimToken.toBuilder().claimed(true).upiId(SOME_UPI_ID).build();
        given(claimTokenRepository.save(updatedClaim)).willReturn(updatedClaim);

        willThrow(new RuntimeException("Temporal unavailable"))
                .given(claimSignaler).signalClaim(SOME_REMITTANCE_ID, SOME_TOKEN, SOME_UPI_ID);

        // when / then
        assertThatThrownBy(() -> submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Temporal unavailable");
    }

    @Test
    void shouldSubmitClaimWithoutSignalerWhenNotConfigured() {
        // given
        var handler = new SubmitClaimHandler(
                claimTokenRepository, remittanceRepository, Optional.empty());

        var claimToken = claimTokenBuilder().build();
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .build();

        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID)).willReturn(Optional.of(remittance));

        var updatedClaim = claimToken.toBuilder().claimed(true).upiId(SOME_UPI_ID).build();
        given(claimTokenRepository.save(updatedClaim)).willReturn(updatedClaim);

        // when
        var result = handler.handle(SOME_TOKEN, SOME_UPI_ID);

        // then
        var expected = ClaimDetails.builder()
                .claimToken(updatedClaim)
                .remittance(remittance)
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(claimSignaler).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        // given
        given(claimTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> submitClaimHandler.handle("unknown-token", SOME_UPI_ID))
                .isInstanceOf(ClaimTokenNotFoundException.class)
                .hasMessageContaining("SP-0011");
    }

    @Test
    void shouldThrowWhenAlreadyClaimed() {
        // given
        var claimToken = claimTokenBuilder().claimed(true).build();
        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));

        // when / then
        assertThatThrownBy(() -> submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID))
                .isInstanceOf(ClaimAlreadyClaimedException.class)
                .hasMessageContaining("SP-0012");
    }

    @Test
    void shouldThrowWhenRemittanceNotEscrowed() {
        // given
        var claimToken = claimTokenBuilder().build();
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.CANCELLED)
                .build();

        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID)).willReturn(Optional.of(remittance));

        // when / then
        assertThatThrownBy(() -> submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID))
                .isInstanceOf(InvalidRemittanceStateException.class)
                .hasMessageContaining("SP-0014");
    }

    @Test
    void shouldThrowWhenTokenExpired() {
        // given
        var claimToken = claimTokenBuilder()
                .expiresAt(SOME_EXPIRED_AT)
                .build();
        given(claimTokenRepository.findByToken(SOME_TOKEN)).willReturn(Optional.of(claimToken));

        // when / then
        assertThatThrownBy(() -> submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID))
                .isInstanceOf(ClaimTokenExpiredException.class)
                .hasMessageContaining("SP-0013");
    }
}
