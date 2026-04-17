package com.stablepay.application.controller.funding;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.controller.funding.mapper.FundingApiMapper;
import com.stablepay.application.dto.FundWalletRequest;
import com.stablepay.application.dto.FundingOrderResponse;
import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.handler.GetFundingOrderHandler;
import com.stablepay.domain.funding.handler.InitiateFundingHandler;
import com.stablepay.domain.funding.model.FundingInitiationResult;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.testutil.TestClockConfig;

import lombok.SneakyThrows;

@WebMvcTest(FundingController.class)
@Import(TestClockConfig.class)
class FundingControllerTest {

    private static final String SOME_CLIENT_SECRET = "pi_3MnTest_secret_abc";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitiateFundingHandler initiateFundingHandler;

    @MockitoBean
    private GetFundingOrderHandler getFundingOrderHandler;

    @MockitoBean
    private FundingApiMapper fundingApiMapper;

    @Test
    @SneakyThrows
    void shouldInitiateFundingAndReturnClientSecret() {
        // given
        var order = fundingOrderBuilder().build();
        var result = FundingInitiationResult.builder()
                .order(order)
                .clientSecret(SOME_CLIENT_SECRET)
                .build();

        var responseWithSecret = FundingOrderResponse.builder()
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .stripeClientSecret(SOME_CLIENT_SECRET)
                .createdAt(SOME_CREATED_AT)
                .build();

        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC)).willReturn(result);
        given(fundingApiMapper.toResponseWithClientSecret(order, SOME_CLIENT_SECRET))
                .willReturn(responseWithSecret);

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fundingId").value(SOME_FUNDING_ID.toString()))
                .andExpect(jsonPath("$.walletId").value(SOME_WALLET_ID))
                .andExpect(jsonPath("$.amountUsdc").value(SOME_AMOUNT_USDC.doubleValue()))
                .andExpect(jsonPath("$.status").value("PAYMENT_CONFIRMED"))
                .andExpect(jsonPath("$.stripePaymentIntentId").value(SOME_STRIPE_PAYMENT_INTENT_ID))
                .andExpect(jsonPath("$.stripeClientSecret").value(SOME_CLIENT_SECRET));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenWalletNotFound() {
        // given
        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .willThrow(WalletNotFoundException.byId(SOME_WALLET_ID));

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0006"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WhenFundingAlreadyInProgress() {
        // given
        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .willThrow(FundingAlreadyInProgressException.forWallet(SOME_WALLET_ID));

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0022"));
    }

    @Test
    @SneakyThrows
    void shouldReturn502WhenStripeFails() {
        // given
        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .willThrow(FundingFailedException.stripeError("card_declined", new RuntimeException()));

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("SP-0021"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenAmountBelowMinimum() {
        // given
        var request = FundWalletRequest.builder().amount(new BigDecimal("0.50")).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenAmountAboveMaximum() {
        // given
        var request = FundWalletRequest.builder().amount(new BigDecimal("10000.01")).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenAmountExceedsTwoDecimalPlaces() {
        // given
        var request = FundWalletRequest.builder().amount(new BigDecimal("25.123")).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }

    @Test
    @SneakyThrows
    void shouldReturnFundingOrderStatusWithoutClientSecret() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        var response = FundingOrderResponse.builder()
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.FUNDED)
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .createdAt(SOME_CREATED_AT)
                .build();

        given(getFundingOrderHandler.handle(SOME_FUNDING_ID)).willReturn(order);
        given(fundingApiMapper.toResponse(order)).willReturn(response);

        // when
        var result = mockMvc.perform(get("/api/funding-orders/{fundingId}", SOME_FUNDING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingId").value(SOME_FUNDING_ID.toString()))
                .andExpect(jsonPath("$.status").value("FUNDED"))
                .andExpect(jsonPath("$.stripePaymentIntentId").value(SOME_STRIPE_PAYMENT_INTENT_ID))
                .andReturn();

        // then
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("stripeClientSecret");
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenFundingOrderNotFound() {
        // given
        var missingId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        given(getFundingOrderHandler.handle(missingId))
                .willThrow(FundingOrderNotFoundException.byFundingId(missingId));

        // when / then
        mockMvc.perform(get("/api/funding-orders/{fundingId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0020"));
    }
}
