package com.stablepay.application.controller.funding;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import com.stablepay.application.config.SecurityAuthenticationEntryPoint;
import com.stablepay.application.controller.funding.mapper.FundingApiMapper;
import com.stablepay.application.dto.FundWalletRequest;
import com.stablepay.application.dto.FundingOrderResponse;
import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.exception.InsufficientBalanceForRefundException;
import com.stablepay.domain.funding.exception.RefundFailedException;
import com.stablepay.domain.funding.exception.RefundNotAllowedException;
import com.stablepay.domain.funding.handler.GetFundingOrderHandler;
import com.stablepay.domain.funding.handler.InitiateFundingHandler;
import com.stablepay.domain.funding.handler.RefundFundingHandler;
import com.stablepay.domain.funding.model.FundingInitiationResult;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(FundingController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
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
    private RefundFundingHandler refundFundingHandler;

    @MockitoBean
    private FundingApiMapper fundingApiMapper;

    @MockitoBean
    private SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

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

        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID)).willReturn(result);
        given(fundingApiMapper.toResponseWithClientSecret(order, SOME_CLIENT_SECRET))
                .willReturn(responseWithSecret);

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID)))
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
        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID))
                .willThrow(WalletNotFoundException.byId(SOME_WALLET_ID));

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0006"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WhenFundingAlreadyInProgress() {
        // given
        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID))
                .willThrow(FundingAlreadyInProgressException.forWallet(SOME_WALLET_ID));

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0022"));
    }

    @Test
    @SneakyThrows
    void shouldReturn502WhenStripeFails() {
        // given
        given(initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID))
                .willThrow(FundingFailedException.stripeError("card_declined", new RuntimeException()));

        var request = FundWalletRequest.builder().amount(SOME_AMOUNT_USDC).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID)))
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
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID)))
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
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID)))
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
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID)))
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

        given(getFundingOrderHandler.handle(SOME_FUNDING_ID, SOME_USER_ID)).willReturn(order);
        given(fundingApiMapper.toResponse(order)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/funding-orders/{fundingId}", SOME_FUNDING_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingId").value(SOME_FUNDING_ID.toString()))
                .andExpect(jsonPath("$.status").value("FUNDED"))
                .andExpect(jsonPath("$.stripePaymentIntentId").value(SOME_STRIPE_PAYMENT_INTENT_ID))
                .andExpect(jsonPath("$.stripeClientSecret").doesNotExist());
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenFundingOrderNotFound() {
        // given
        var missingId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        given(getFundingOrderHandler.handle(missingId, SOME_USER_ID))
                .willThrow(FundingOrderNotFoundException.byFundingId(missingId));

        // when / then
        mockMvc.perform(get("/api/funding-orders/{fundingId}", missingId)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0020"));
    }

    @Test
    @SneakyThrows
    void shouldRefundFundedOrderAndReturnUpdatedStatus() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.REFUNDED).build();
        var response = FundingOrderResponse.builder()
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.REFUNDED)
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .createdAt(SOME_CREATED_AT)
                .build();

        given(refundFundingHandler.handle(SOME_FUNDING_ID, SOME_USER_ID)).willReturn(order);
        given(fundingApiMapper.toResponse(order)).willReturn(response);

        // when / then
        mockMvc.perform(post("/api/funding-orders/{fundingId}/refund", SOME_FUNDING_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingId").value(SOME_FUNDING_ID.toString()))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.stripeClientSecret").doesNotExist());
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenRefundingMissingOrder() {
        // given
        given(refundFundingHandler.handle(SOME_FUNDING_ID, SOME_USER_ID))
                .willThrow(FundingOrderNotFoundException.byFundingId(SOME_FUNDING_ID));

        // when / then
        mockMvc.perform(post("/api/funding-orders/{fundingId}/refund", SOME_FUNDING_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0020"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WhenRefundNotAllowed() {
        // given
        given(refundFundingHandler.handle(SOME_FUNDING_ID, SOME_USER_ID))
                .willThrow(RefundNotAllowedException.forStatus(FundingStatus.PAYMENT_CONFIRMED));

        // when / then
        mockMvc.perform(post("/api/funding-orders/{fundingId}/refund", SOME_FUNDING_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0023"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WhenRefundBalanceInsufficient() {
        // given
        given(refundFundingHandler.handle(SOME_FUNDING_ID, SOME_USER_ID))
                .willThrow(InsufficientBalanceForRefundException.forAmount(
                        SOME_AMOUNT_USDC, new BigDecimal("0.00")));

        // when / then
        mockMvc.perform(post("/api/funding-orders/{fundingId}/refund", SOME_FUNDING_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0025"));
    }

    @Test
    @SneakyThrows
    void shouldReturn502WhenStripeRefundFails() {
        // given
        given(refundFundingHandler.handle(SOME_FUNDING_ID, SOME_USER_ID))
                .willThrow(RefundFailedException.stripeRefundFailed(
                        SOME_STRIPE_PAYMENT_INTENT_ID, new RuntimeException("boom")));

        // when / then
        mockMvc.perform(post("/api/funding-orders/{fundingId}/refund", SOME_FUNDING_ID)
                        .with(authentication(TestSecurityConfig.authenticationFor(SOME_USER_ID))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("SP-0024"));
    }
}
