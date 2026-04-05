package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.FxQuoteFixtures.fxQuoteBuilder;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Captor
    private ArgumentCaptor<Remittance> remittanceCaptor;

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
                        && r.status() == RemittanceStatus.INITIATED)))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0).toBuilder().id(1L).build());

        // when
        createRemittanceHandler.handle(SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC);

        // then
        then(remittanceRepository).should().save(remittanceCaptor.capture());

        var expected = Remittance.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .build();

        assertThat(remittanceCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("remittanceId")
                .isEqualTo(expected);

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
