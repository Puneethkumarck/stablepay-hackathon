package com.stablepay.infrastructure.razorpay;

import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.model.DisbursementResult;

class RazorpayUpiDisbursementAdapterTest {

    private static final String BASE_URL = "https://razorpay.test/v1";
    private static final String ACCOUNT_NUMBER = "1234567890";
    private static final String REMITTANCE_ID = SOME_REMITTANCE_ID.toString();
    private static final String CONTACT_ID = "cont_ABC123";
    private static final String FUND_ACCOUNT_ID = "fa_ABC123";
    private static final String PAYOUT_ID = "pout_ABC123";

    private MockRestServiceServer mockServer;
    private RazorpayUpiDisbursementAdapter adapter;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        var properties = RazorpayDisbursementProperties.builder()
                .apiKeyId("rzp_test_key")
                .apiKeySecret("secret")
                .accountNumber(ACCOUNT_NUMBER)
                .baseUrl(BASE_URL)
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        adapter = new RazorpayUpiDisbursementAdapter(builder.build(), properties);
    }

    @Test
    void shouldCreateContactFundAccountAndPayoutOnHappyPath() {
        // given
        mockServer.expect(requestTo(BASE_URL + "/contacts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"name":"StablePay Recipient","type":"customer","reference_id":"%s"}
                        """.formatted(REMITTANCE_ID)))
                .andRespond(withSuccess("""
                        {"id":"%s","entity":"contact"}
                        """.formatted(CONTACT_ID), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/fund_accounts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"contact_id":"%s","account_type":"vpa","vpa":{"address":"%s"}}
                        """.formatted(CONTACT_ID, SOME_UPI_ID)))
                .andRespond(withSuccess("""
                        {"id":"%s","entity":"fund_account"}
                        """.formatted(FUND_ACCOUNT_ID), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Payout-Idempotency", REMITTANCE_ID))
                .andExpect(content().json("""
                        {
                          "account_number":"%s",
                          "fund_account_id":"%s",
                          "amount":850000,
                          "currency":"INR",
                          "mode":"UPI",
                          "purpose":"payout",
                          "queue_if_low_balance":false,
                          "reference_id":"%s",
                          "narration":"StablePay remittance"
                        }
                        """.formatted(ACCOUNT_NUMBER, FUND_ACCOUNT_ID, REMITTANCE_ID)))
                .andRespond(withSuccess("""
                        {"id":"%s","status":"processing","entity":"payout"}
                        """.formatted(PAYOUT_ID), MediaType.APPLICATION_JSON));

        // when
        var result = adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID);

        // then
        var expected = DisbursementResult.builder()
                .providerId(PAYOUT_ID)
                .providerStatus("processing")
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        mockServer.verify();
    }

    @Test
    void shouldSendIdempotencyHeaderAsBareRemittanceUuidThirtySixCharsLong() {
        // given
        assertThat(REMITTANCE_ID).hasSize(36);
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andExpect(header("X-Payout-Idempotency", REMITTANCE_ID))
                .andRespond(withSuccess("""
                        {"id":"%s","status":"processing"}
                        """.formatted(PAYOUT_ID), MediaType.APPLICATION_JSON));

        // when
        adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID);

        // then
        mockServer.verify();
    }

    @Test
    void shouldThrowNonRetriableWhenAmountBelowMinimumWithoutHttpCall() {
        // given
        var belowMinimum = new BigDecimal("0.99");

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, belowMinimum, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("SP-0018")
                .hasMessageContaining("below minimum");
        mockServer.verify();
    }

    @Test
    void shouldThrowNonRetriableWhenPayoutReturnsFourHundred() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":"BAD_REQUEST_ERROR","description":"Invalid VPA"}}
                                """));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("SP-0018")
                .hasMessageContaining("BAD_REQUEST_ERROR")
                .hasMessageContaining("Invalid VPA");
        mockServer.verify();
    }

    @Test
    void shouldThrowRetriableWhenPayoutReturnsFiveZeroThree() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.class)
                .isNotInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("SP-0018");
        mockServer.verify();
    }

    @Test
    void shouldThrowRetriableWhenPayoutReturnsFiveHundred() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withServerError());

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.class)
                .isNotInstanceOf(DisbursementException.NonRetriable.class);
        mockServer.verify();
    }

    @Test
    void shouldThrowRetriableWhenPayoutTimesOut() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withException(new IOException("connect timed out")));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.class)
                .isNotInstanceOf(DisbursementException.NonRetriable.class);
        mockServer.verify();
    }

    @Test
    void shouldThrowNonRetriableWhenContactCreationRejectsUpi() {
        // given
        mockServer.expect(requestTo(BASE_URL + "/contacts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":"BAD_REQUEST_ERROR","description":"contact invalid"}}
                                """));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("createContact")
                .hasMessageContaining("BAD_REQUEST_ERROR");
        mockServer.verify();
    }

    @Test
    void shouldSendAmountInPaiseEqualToInrTimesOneHundred() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andExpect(content().json("""
                        {"amount":850000,"currency":"INR"}
                        """))
                .andRespond(withSuccess("""
                        {"id":"%s","status":"processing"}
                        """.formatted(PAYOUT_ID), MediaType.APPLICATION_JSON));

        // when
        adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID);

        // then
        mockServer.verify();
    }

    @Test
    void shouldThrowNonRetriableWhenPayoutResponseHasNoId() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withSuccess("""
                        {"status":"processing"}
                        """, MediaType.APPLICATION_JSON));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("no id");
        mockServer.verify();
    }

    @Test
    void shouldThrowRetriableWhenPayoutReturnsFourTwentyNineRateLimited() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":"RATE_LIMIT_EXCEEDED","description":"Too many requests"}}
                                """));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.class)
                .isNotInstanceOf(DisbursementException.NonRetriable.class);
        mockServer.verify();
    }

    @Test
    void shouldThrowRetriableWhenPayoutReturnsFourOhEightRequestTimeout() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withStatus(HttpStatus.REQUEST_TIMEOUT));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.class)
                .isNotInstanceOf(DisbursementException.NonRetriable.class);
        mockServer.verify();
    }

    @Test
    void shouldMaskUpiInExceptionMessageWhenRazorpayEchoesVpaInErrorDescription() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":"BAD_REQUEST_ERROR","description":"vpa.address alice@hdfcbank is invalid"}}
                                """));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("BAD_REQUEST_ERROR")
                .hasMessageNotContaining("alice@hdfcbank");
        mockServer.verify();
    }

    @Test
    void shouldNotLeakRawBodyWhenErrorResponseIsNotJson() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html><body>Bad request from sensitive@host</body></html>"));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageNotContaining("<html>")
                .hasMessageNotContaining("sensitive@host");
        mockServer.verify();
    }

    @Test
    void shouldTolerateRazorpayErrorBodyWithNullCodeAndDescription() {
        // given
        stubContactAndFundAccount();
        mockServer.expect(requestTo(BASE_URL + "/payouts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":null,"description":null}}
                                """));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("UNKNOWN")
                .hasMessageContaining("no description");
        mockServer.verify();
    }

    @Test
    void shouldThrowNonRetriableWhenFundAccountCreationFails() {
        // given
        mockServer.expect(requestTo(BASE_URL + "/contacts"))
                .andRespond(withSuccess("""
                        {"id":"%s"}
                        """.formatted(CONTACT_ID), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/fund_accounts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":"BAD_REQUEST_ERROR","description":"Invalid VPA handle"}}
                                """));

        // when / then
        assertThatThrownBy(() ->
                adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("createFundAccount")
                .hasMessageContaining("BAD_REQUEST_ERROR");
        mockServer.verify();
    }

    private void stubContactAndFundAccount() {
        mockServer.expect(requestTo(BASE_URL + "/contacts"))
                .andRespond(withSuccess("""
                        {"id":"%s"}
                        """.formatted(CONTACT_ID), MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/fund_accounts"))
                .andRespond(withSuccess("""
                        {"id":"%s"}
                        """.formatted(FUND_ACCOUNT_ID), MediaType.APPLICATION_JSON));
    }
}
