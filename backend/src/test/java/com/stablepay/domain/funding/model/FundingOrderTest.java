package com.stablepay.domain.funding.model;

import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FundingOrderTest {

    @Test
    void shouldAcceptNullForOptionalFields() {
        // given
        var builder = fundingOrderBuilder()
                .id(null)
                .stripePaymentIntentId(null)
                .createdAt(null)
                .updatedAt(null);

        // when
        var actual = builder.build();

        // then
        var expected = fundingOrderBuilder()
                .id(null)
                .stripePaymentIntentId(null)
                .createdAt(null)
                .updatedAt(null)
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnCopyWithUpdatedStatusViaToBuilder() {
        // given
        var original = fundingOrderBuilder().build();

        // when
        var actual = original.toBuilder().status(FundingStatus.FUNDED).build();

        // then
        var expected = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenFundingIdIsNull() {
        // given
        var builder = fundingOrderBuilder().fundingId(null);

        // when / then
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fundingId");
    }

    @Test
    void shouldThrowWhenWalletIdIsNull() {
        // given
        var builder = fundingOrderBuilder().walletId(null);

        // when / then
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void shouldThrowWhenAmountUsdcIsNull() {
        // given
        var builder = fundingOrderBuilder().amountUsdc(null);

        // when / then
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amountUsdc");
    }

    @Test
    void shouldThrowWhenStatusIsNull() {
        // given
        var builder = fundingOrderBuilder().status(null);

        // when / then
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }
}
