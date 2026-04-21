package com.stablepay.application.controller.claim;

import static com.stablepay.testutil.AuthFixtures.SOME_SENDER_DISPLAY_NAME;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_TOKEN;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_UPI_ID;
import static com.stablepay.testutil.ClaimTokenFixtures.claimTokenBuilder;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.controller.claim.mapper.ClaimApiMapper;
import com.stablepay.application.controller.claim.mapper.ClaimApiMapperImpl;
import com.stablepay.application.dto.SubmitClaimRequest;
import com.stablepay.domain.claim.exception.ClaimAlreadyClaimedException;
import com.stablepay.domain.claim.exception.ClaimTokenExpiredException;
import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.claim.handler.GetClaimQueryHandler;
import com.stablepay.domain.claim.handler.SubmitClaimHandler;
import com.stablepay.domain.claim.model.ClaimDetails;
import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(ClaimController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class, ClaimApiMapperImpl.class})
class ClaimControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetClaimQueryHandler getClaimQueryHandler;

    @MockitoBean
    private SubmitClaimHandler submitClaimHandler;

    @MockitoSpyBean
    private ClaimApiMapper claimApiMapper;

    @Test
    @SneakyThrows
    void shouldGetClaimDetails() {
        // given
        var claimToken = claimTokenBuilder().build();
        var remittance = remittanceBuilder().build();
        var claimDetails = ClaimDetails.builder()
                .claimToken(claimToken)
                .remittance(remittance)
                .senderDisplayName(SOME_SENDER_DISPLAY_NAME)
                .build();

        given(getClaimQueryHandler.handle(SOME_TOKEN)).willReturn(claimDetails);

        // when / then
        mockMvc.perform(get("/api/claims/{token}", SOME_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remittanceId").value(SOME_REMITTANCE_ID.toString()))
                .andExpect(jsonPath("$.senderDisplayName").value(SOME_SENDER_DISPLAY_NAME))
                .andExpect(jsonPath("$.amountUsdc").value(SOME_AMOUNT_USDC.intValue()))
                .andExpect(jsonPath("$.amountInr").value(SOME_AMOUNT_INR.doubleValue()))
                .andExpect(jsonPath("$.fxRate").value(SOME_FX_RATE.doubleValue()))
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.claimed").value(false));
    }

    @Test
    @SneakyThrows
    void shouldSubmitClaim() {
        // given
        var claimToken = claimTokenBuilder().claimed(true).build();
        var remittance = remittanceBuilder().status(RemittanceStatus.CLAIMED).build();
        var claimDetails = ClaimDetails.builder()
                .claimToken(claimToken)
                .remittance(remittance)
                .senderDisplayName(SOME_SENDER_DISPLAY_NAME)
                .build();

        given(submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID)).willReturn(claimDetails);

        var request = SubmitClaimRequest.builder().upiId(SOME_UPI_ID).build();

        // when / then
        mockMvc.perform(post("/api/claims/{token}", SOME_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"))
                .andExpect(jsonPath("$.claimed").value(true));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenTokenNotFound() {
        // given
        given(getClaimQueryHandler.handle("unknown-token"))
                .willThrow(ClaimTokenNotFoundException.byToken("unknown-token"));

        // when / then
        mockMvc.perform(get("/api/claims/{token}", "unknown-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0011"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WhenAlreadyClaimed() {
        // given
        given(submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID))
                .willThrow(ClaimAlreadyClaimedException.forToken(SOME_TOKEN));

        var request = SubmitClaimRequest.builder().upiId(SOME_UPI_ID).build();

        // when / then
        mockMvc.perform(post("/api/claims/{token}", SOME_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0012"));
    }

    @Test
    @SneakyThrows
    void shouldReturn410WhenTokenExpired() {
        // given
        given(submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID))
                .willThrow(ClaimTokenExpiredException.forToken(SOME_TOKEN));

        var request = SubmitClaimRequest.builder().upiId(SOME_UPI_ID).build();

        // when / then
        mockMvc.perform(post("/api/claims/{token}", SOME_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("SP-0013"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WhenRemittanceNotEscrowed() {
        // given
        given(submitClaimHandler.handle(SOME_TOKEN, SOME_UPI_ID))
                .willThrow(InvalidRemittanceStateException.forClaim(RemittanceStatus.CANCELLED));

        var request = SubmitClaimRequest.builder().upiId(SOME_UPI_ID).build();

        // when / then
        mockMvc.perform(post("/api/claims/{token}", SOME_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0014"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenUpiIdInvalid() {
        // given
        var request = SubmitClaimRequest.builder().upiId("invalid-no-at-sign").build();

        // when / then
        mockMvc.perform(post("/api/claims/{token}", SOME_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenUpiIdEmpty() {
        // given
        var request = SubmitClaimRequest.builder().upiId("").build();

        // when / then
        mockMvc.perform(post("/api/claims/{token}", SOME_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }
}
