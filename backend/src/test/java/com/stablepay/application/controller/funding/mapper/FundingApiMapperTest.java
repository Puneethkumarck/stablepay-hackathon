package com.stablepay.application.controller.funding.mapper;

import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.application.dto.FundingOrderResponse;

class FundingApiMapperTest {

    private static final String SOME_CLIENT_SECRET = "pi_3MnTest_secret_abc";

    private final FundingApiMapper mapper = new FundingApiMapperImpl();

    @Test
    void shouldMapFundingOrderToResponseWithoutClientSecret() {
        // given
        var order = fundingOrderBuilder().build();

        // when
        var response = mapper.toResponse(order);

        // then
        var expected = FundingOrderResponse.builder()
                .fundingId(order.fundingId())
                .walletId(order.walletId())
                .amountUsdc(order.amountUsdc())
                .status(order.status())
                .stripePaymentIntentId(order.stripePaymentIntentId())
                .createdAt(order.createdAt())
                .build();

        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapFundingOrderToResponseWithClientSecretWhenProvided() {
        // given
        var order = fundingOrderBuilder().build();

        // when
        var response = mapper.toResponseWithClientSecret(order, SOME_CLIENT_SECRET);

        // then
        var expected = FundingOrderResponse.builder()
                .fundingId(order.fundingId())
                .walletId(order.walletId())
                .amountUsdc(order.amountUsdc())
                .status(order.status())
                .stripePaymentIntentId(order.stripePaymentIntentId())
                .stripeClientSecret(SOME_CLIENT_SECRET)
                .createdAt(order.createdAt())
                .build();

        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }
}
