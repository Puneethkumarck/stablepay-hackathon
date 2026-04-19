package com.stablepay.infrastructure.razorpay;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.stablepay.domain.common.port.SmsProvider;
import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.test.PostgresContainerExtension;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(PostgresContainerExtension.class)
@Import(RazorpayUpiDisbursementAdapterIntegrationTest.InfrastructureMocksConfig.class)
class RazorpayUpiDisbursementAdapterIntegrationTest {

    @TestConfiguration
    static class InfrastructureMocksConfig {

        @Bean
        MpcWalletClient mpcWalletClient() {
            return Mockito.mock(MpcWalletClient.class);
        }

        @Bean
        SmsProvider smsProvider() {
            return Mockito.mock(SmsProvider.class);
        }
    }

    private static final String REMITTANCE_ID = SOME_REMITTANCE_ID.toString();

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("stablepay.disbursement.provider", () -> "razorpay");
        registry.add("stablepay.razorpay.api-key-id", () -> "rzp_test_dummy");
        registry.add("stablepay.razorpay.api-key-secret", () -> "dummy_secret");
        registry.add("stablepay.razorpay.account-number", () -> "1234567890");
        registry.add("stablepay.razorpay.base-url", wireMock::baseUrl);
        registry.add("stablepay.razorpay.request-timeout", () -> "PT5S");
    }

    @Autowired
    private FiatDisbursementProvider fiatDisbursementProvider;

    @Test
    void shouldWireRazorpayAdapterWhenProviderPropertySet() {
        // given / when / then
        assertThat(fiatDisbursementProvider).isInstanceOf(RazorpayUpiDisbursementAdapter.class);
    }

    @Test
    void shouldIssueContactFundAccountAndPayoutRequestsWithIdempotencyHeader() {
        // given
        wireMock.stubFor(post(urlEqualTo("/contacts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"cont_wm_1"}
                                """)));
        wireMock.stubFor(post(urlEqualTo("/fund_accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"fa_wm_1"}
                                """)));
        wireMock.stubFor(post(urlEqualTo("/payouts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"pout_wm_1","status":"processing"}
                                """)));

        // when
        var result = fiatDisbursementProvider.disburse(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID);

        // then
        var expected = DisbursementResult.builder()
                .providerId("pout_wm_1")
                .providerStatus("processing")
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);

        wireMock.verify(postRequestedFor(urlEqualTo("/payouts"))
                .withHeader("X-Payout-Idempotency", equalTo(REMITTANCE_ID))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("850000")))
                .withRequestBody(matchingJsonPath("$.currency", equalTo("INR")))
                .withRequestBody(matchingJsonPath("$.reference_id", equalTo(REMITTANCE_ID))));
    }

    @Test
    void shouldThrowNonRetriableWhenRazorpayReturnsFourHundred() {
        // given
        wireMock.stubFor(post(urlEqualTo("/contacts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"cont_wm_1"}
                                """)));
        wireMock.stubFor(post(urlEqualTo("/fund_accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"fa_wm_1"}
                                """)));
        wireMock.stubFor(post(urlEqualTo("/payouts"))
                .withRequestBody(equalToJson("""
                        {"amount":850000,"currency":"INR"}
                        """, true, true))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error":{"code":"BAD_REQUEST_ERROR","description":"Invalid VPA"}}
                                """)));

        // when / then
        assertThatThrownBy(() -> fiatDisbursementProvider.disburse(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.NonRetriable.class)
                .hasMessageContaining("BAD_REQUEST_ERROR");
    }

    @Test
    void shouldThrowRetriableWhenRazorpayReturnsFiveOhThree() {
        // given
        wireMock.stubFor(post(urlEqualTo("/contacts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"cont_wm_1"}
                                """)));
        wireMock.stubFor(post(urlEqualTo("/fund_accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"fa_wm_1"}
                                """)));
        wireMock.stubFor(post(urlEqualTo("/payouts"))
                .willReturn(aResponse().withStatus(503)));

        // when / then
        assertThatThrownBy(() -> fiatDisbursementProvider.disburse(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, REMITTANCE_ID))
                .isInstanceOf(DisbursementException.class)
                .isNotInstanceOf(DisbursementException.NonRetriable.class);
    }
}
