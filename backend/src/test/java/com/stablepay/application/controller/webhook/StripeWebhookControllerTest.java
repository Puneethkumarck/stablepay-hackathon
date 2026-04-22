package com.stablepay.application.controller.webhook;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.WebhookEventFixtures.SOME_PAYMENT_FAILED_EVENT;
import static com.stablepay.testutil.WebhookEventFixtures.SOME_PAYMENT_SUCCEEDED_EVENT;
import static com.stablepay.testutil.WebhookEventFixtures.SOME_UNKNOWN_EVENT;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.stablepay.domain.funding.exception.InvalidWebhookSignatureException;
import com.stablepay.domain.funding.handler.CompleteFundingHandler;
import com.stablepay.domain.funding.handler.FailFundingHandler;
import com.stablepay.domain.funding.port.PaymentWebhookVerifier;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(StripeWebhookController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
class StripeWebhookControllerTest {

    private static final String SOME_PAYLOAD = "{\"id\":\"evt_test_0123456789\"}";
    private static final String SOME_SIGNATURE_HEADER = "t=1700000000,v1=deadbeef";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentWebhookVerifier paymentWebhookVerifier;

    @MockitoBean
    private CompleteFundingHandler completeFundingHandler;

    @MockitoBean
    private FailFundingHandler failFundingHandler;

    @Test
    @SneakyThrows
    void shouldReturn400WhenSignatureVerificationFails() {
        // given
        given(paymentWebhookVerifier.verify(SOME_PAYLOAD, SOME_SIGNATURE_HEADER))
                .willThrow(InvalidWebhookSignatureException.withReason("tampered signature"));

        // when
        var resultActions = mockMvc.perform(post("/webhooks/stripe")
                .header("Stripe-Signature", SOME_SIGNATURE_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SOME_PAYLOAD));

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0026"));
        then(completeFundingHandler).shouldHaveNoInteractions();
        then(failFundingHandler).shouldHaveNoInteractions();
    }

    @Test
    @SneakyThrows
    void shouldDispatchToCompleteHandlerOnPaymentSucceeded() {
        // given
        given(paymentWebhookVerifier.verify(SOME_PAYLOAD, SOME_SIGNATURE_HEADER))
                .willReturn(SOME_PAYMENT_SUCCEEDED_EVENT);

        // when
        var resultActions = mockMvc.perform(post("/webhooks/stripe")
                .header("Stripe-Signature", SOME_SIGNATURE_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SOME_PAYLOAD));

        // then
        resultActions.andExpect(status().isOk());
        then(completeFundingHandler).should().handle(SOME_FUNDING_ID);
        then(failFundingHandler).shouldHaveNoInteractions();
    }

    @Test
    @SneakyThrows
    void shouldDispatchToFailHandlerOnPaymentFailed() {
        // given
        given(paymentWebhookVerifier.verify(SOME_PAYLOAD, SOME_SIGNATURE_HEADER))
                .willReturn(SOME_PAYMENT_FAILED_EVENT);

        // when
        var resultActions = mockMvc.perform(post("/webhooks/stripe")
                .header("Stripe-Signature", SOME_SIGNATURE_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SOME_PAYLOAD));

        // then
        resultActions.andExpect(status().isOk());
        then(failFundingHandler).should().handle(SOME_FUNDING_ID);
        then(completeFundingHandler).shouldHaveNoInteractions();
    }

    @Test
    @SneakyThrows
    void shouldReturn200OnUnknownEventType() {
        // given
        given(paymentWebhookVerifier.verify(SOME_PAYLOAD, SOME_SIGNATURE_HEADER))
                .willReturn(SOME_UNKNOWN_EVENT);

        // when
        var resultActions = mockMvc.perform(post("/webhooks/stripe")
                .header("Stripe-Signature", SOME_SIGNATURE_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SOME_PAYLOAD));

        // then
        resultActions.andExpect(status().isOk());
        then(completeFundingHandler).shouldHaveNoInteractions();
        then(failFundingHandler).shouldHaveNoInteractions();
    }

    @Test
    @SneakyThrows
    void shouldReturn200WhenHandlerThrows() {
        // given
        given(paymentWebhookVerifier.verify(SOME_PAYLOAD, SOME_SIGNATURE_HEADER))
                .willReturn(SOME_PAYMENT_SUCCEEDED_EVENT);
        willThrow(new RuntimeException("boom"))
                .given(completeFundingHandler).handle(SOME_FUNDING_ID);

        // when
        var resultActions = mockMvc.perform(post("/webhooks/stripe")
                .header("Stripe-Signature", SOME_SIGNATURE_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SOME_PAYLOAD));

        // then
        resultActions.andExpect(status().isOk());
        then(completeFundingHandler).should().handle(SOME_FUNDING_ID);
        then(failFundingHandler).shouldHaveNoInteractions();
    }
}
