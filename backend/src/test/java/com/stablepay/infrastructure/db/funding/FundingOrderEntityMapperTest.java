package com.stablepay.infrastructure.db.funding;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ORDER_DB_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_UPDATED_AT;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.funding.model.FundingStatus;

class FundingOrderEntityMapperTest {

    private final FundingOrderEntityMapper mapper = new FundingOrderEntityMapperImpl();

    @Test
    void shouldMapDomainToEntityAndBack() {
        // given
        var domain = fundingOrderBuilder().build();

        // when
        var entity = mapper.toEntity(domain);
        var backToDomain = mapper.toDomain(entity);

        // then
        assertThat(backToDomain)
                .usingRecursiveComparison()
                .isEqualTo(domain);
    }

    @Test
    void shouldMapEntityToDomainWithAllFields() {
        // given
        var entity = FundingOrderEntity.builder()
                .id(SOME_FUNDING_ORDER_DB_ID)
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        // when
        var domain = mapper.toDomain(entity);

        // then
        var expected = fundingOrderBuilder().build();
        assertThat(domain)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
