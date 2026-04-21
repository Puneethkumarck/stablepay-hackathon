package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.AuthFixtures.SOME_OTHER_USER_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
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
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class GetFundingOrderHandlerTest {

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GetFundingOrderHandler getFundingOrderHandler;

    @Test
    void shouldReturnFundingOrderWhenFound() {
        // given
        var order = fundingOrderBuilder().build();
        var wallet = walletBuilder().id(order.walletId()).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(order.walletId())).willReturn(Optional.of(wallet));

        // when
        var result = getFundingOrderHandler.handle(SOME_FUNDING_ID, SOME_USER_ID);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(order);
    }

    @Test
    void shouldThrowWhenFundingOrderNotFound() {
        // given
        var missingId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        given(fundingOrderRepository.findByFundingId(missingId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> getFundingOrderHandler.handle(missingId, SOME_USER_ID))
                .isInstanceOf(FundingOrderNotFoundException.class)
                .hasMessageContaining("SP-0020")
                .hasMessageContaining(missingId.toString());
    }

    @Test
    void shouldThrowWhenFundingOrderBelongsToDifferentUser() {
        // given
        var order = fundingOrderBuilder().build();
        var wallet = walletBuilder().id(order.walletId()).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(order.walletId())).willReturn(Optional.of(wallet));

        // when / then
        assertThatThrownBy(() -> getFundingOrderHandler.handle(SOME_FUNDING_ID, SOME_OTHER_USER_ID))
                .isInstanceOf(FundingOrderNotFoundException.class)
                .hasMessageContaining("SP-0020");
    }
}
