package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.FxQuoteFixtures.fxQuoteBuilder;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.fx.port.FxRateProvider;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.domain.wallet.exception.InsufficientBalanceException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class CreateRemittanceHandlerTest {

    @Mock
    private RemittanceRepository remittanceRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FxRateProvider fxRateProvider;

    @InjectMocks
    private CreateRemittanceHandler createRemittanceHandler;

    @Test
    void shouldCreateRemittanceWithLockedFxRate() {
        // given
        var wallet = walletBuilder()
                .availableBalance(BigDecimal.valueOf(500))
                .totalBalance(BigDecimal.valueOf(500))
                .build();

        var fxQuote = fxQuoteBuilder().build();

        given(walletRepository.findByUserId(SOME_SENDER_ID)).willReturn(Optional.of(wallet));

        var reservedWallet = wallet.reserveBalance(SOME_AMOUNT_USDC);
        given(walletRepository.save(reservedWallet)).willReturn(reservedWallet);

        given(fxRateProvider.getRate("USD", "INR")).willReturn(fxQuote);

        given(remittanceRepository.save(argThat(r ->
                r.senderId().equals(SOME_SENDER_ID)
                        && r.recipientPhone().equals(SOME_RECIPIENT_PHONE)
                        && r.amountUsdc().compareTo(SOME_AMOUNT_USDC) == 0
                        && r.status() == RemittanceStatus.INITIATED)))
                .willAnswer(invocation -> {
                    Remittance input = invocation.getArgument(0);
                    return input.toBuilder().id(1L).build();
                });

        // when
        var result = createRemittanceHandler.handle(SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC);

        // then
        assertThat(result.senderId()).isEqualTo(SOME_SENDER_ID);
        assertThat(result.recipientPhone()).isEqualTo(SOME_RECIPIENT_PHONE);
        assertThat(result.amountUsdc()).isEqualByComparingTo(SOME_AMOUNT_USDC);
        assertThat(result.amountInr()).isEqualByComparingTo(new BigDecimal("8325.00"));
        assertThat(result.fxRate()).isEqualByComparingTo(fxQuote.rate());
        assertThat(result.status()).isEqualTo(RemittanceStatus.INITIATED);
        assertThat(result.remittanceId()).isNotNull();

        then(walletRepository).should().save(reservedWallet);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // given
        given(walletRepository.findByUserId(SOME_SENDER_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> createRemittanceHandler.handle(
                SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006");
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        // given
        var wallet = walletBuilder()
                .availableBalance(BigDecimal.valueOf(50))
                .totalBalance(BigDecimal.valueOf(50))
                .build();

        given(walletRepository.findByUserId(SOME_SENDER_ID)).willReturn(Optional.of(wallet));

        // when / then
        assertThatThrownBy(() -> createRemittanceHandler.handle(
                SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("SP-0002");
    }
}
