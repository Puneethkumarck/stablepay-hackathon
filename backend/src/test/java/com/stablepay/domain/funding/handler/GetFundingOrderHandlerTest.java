package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.port.FundingOrderRepository;

@ExtendWith(MockitoExtension.class)
class GetFundingOrderHandlerTest {

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @InjectMocks
    private GetFundingOrderHandler getFundingOrderHandler;

    @Test
    void shouldReturnFundingOrderWhenFound() {
        // given
        var order = fundingOrderBuilder().build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        var result = getFundingOrderHandler.handle(SOME_FUNDING_ID);

        // then
        var expected = fundingOrderBuilder().build();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenFundingOrderNotFound() {
        // given
        var missingId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        given(fundingOrderRepository.findByFundingId(missingId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> getFundingOrderHandler.handle(missingId))
                .isInstanceOf(FundingOrderNotFoundException.class)
                .hasMessageContaining("SP-0020")
                .hasMessageContaining(missingId.toString());
    }
}
