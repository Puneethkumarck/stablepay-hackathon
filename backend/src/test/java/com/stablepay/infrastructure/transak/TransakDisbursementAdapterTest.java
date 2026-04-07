package com.stablepay.infrastructure.transak;

import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import com.stablepay.domain.remittance.exception.DisbursementException;

class TransakDisbursementAdapterTest {

    private static final String SOME_AMOUNT_USDC = "100.00";
    private static final String SOME_QUOTE_ID = "quote-abc-123";
    private static final String SOME_ORDER_ID = "order-xyz-789";

    private MockRestServiceServer server;
    private TransakDisbursementAdapter adapter;

    @BeforeEach
    void setUp() {
        var restClientBuilder = RestClient.builder().baseUrl("https://staging-api.transak.com");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        var restClient = restClientBuilder.build();
        var properties = TransakProperties.builder()
                .apiKey("test-api-key")
                .apiSecret("test-api-secret")
                .baseUrl("https://staging-api.transak.com")
                .build();
        adapter = new TransakDisbursementAdapter(restClient, properties);
    }

    @Test
    void shouldCreateQuoteAndOrderOnDisburse() {
        // given
        server.expect(MockRestRequestMatchers.requestTo("https://staging-api.transak.com/api/v1/partners/quotes"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        """
                        {"quoteId":"%s","fiatAmount":"8450.00","cryptoAmount":"100"}
                        """.formatted(SOME_QUOTE_ID),
                        MediaType.APPLICATION_JSON));

        server.expect(MockRestRequestMatchers.requestTo("https://staging-api.transak.com/api/v1/partners/orders"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        """
                        {"orderId":"%s","status":"PENDING","depositAddress":"SoLaNa123"}
                        """.formatted(SOME_ORDER_ID),
                        MediaType.APPLICATION_JSON));

        // when
        adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_REMITTANCE_ID.toString());

        // then
        server.verify();
    }

    @Test
    void shouldThrowDisbursementExceptionWhenQuoteFails() {
        // given
        server.expect(MockRestRequestMatchers.requestTo("https://staging-api.transak.com/api/v1/partners/quotes"))
                .andRespond(MockRestResponseCreators.withServerError());

        // when / then
        assertThatThrownBy(() -> adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_REMITTANCE_ID.toString()))
                .isInstanceOf(DisbursementException.class)
                .hasMessageContaining("SP-0018");

        server.verify();
    }

    @Test
    void shouldThrowDisbursementExceptionWhenOrderFails() {
        // given
        server.expect(MockRestRequestMatchers.requestTo("https://staging-api.transak.com/api/v1/partners/quotes"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        """
                        {"quoteId":"%s","fiatAmount":"8450.00","cryptoAmount":"100"}
                        """.formatted(SOME_QUOTE_ID),
                        MediaType.APPLICATION_JSON));

        server.expect(MockRestRequestMatchers.requestTo("https://staging-api.transak.com/api/v1/partners/orders"))
                .andRespond(MockRestResponseCreators.withServerError());

        // when / then
        assertThatThrownBy(() -> adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_REMITTANCE_ID.toString()))
                .isInstanceOf(DisbursementException.class)
                .hasMessageContaining("SP-0018");

        server.verify();
    }
}
