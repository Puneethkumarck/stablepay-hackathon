package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.FxQuoteFixtures.fxQuoteBuilder;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_NAME;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.claim.model.ClaimToken;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.fx.port.FxRateProvider;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.domain.remittance.port.RemittanceStatusEventRepository;
import com.stablepay.domain.remittance.port.RemittanceWorkflowStarter;
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

    @Mock
    private ClaimTokenRepository claimTokenRepository;

    @Mock
    private RemittanceWorkflowStarter workflowStarter;

    @Mock
    private RemittanceStatusEventRepository remittanceStatusEventRepository;

    @Captor
    private ArgumentCaptor<Remittance> remittanceCaptor;

    @Captor
    private ArgumentCaptor<ClaimToken> claimTokenCaptor;

    private CreateRemittanceHandler createRemittanceHandler;

    @BeforeEach
    void setUp() {
        createRemittanceHandler = new CreateRemittanceHandler(
                remittanceRepository,
                walletRepository,
                fxRateProvider,
                claimTokenRepository,
                Optional.of(workflowStarter),
                remittanceStatusEventRepository);
    }

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

        given(remittanceRepository.save(argThat(r -> r != null && r.senderId() != null)))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0).toBuilder().id(1L).build());

        given(claimTokenRepository.save(argThat(ct -> ct != null && !ct.claimed())))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = createRemittanceHandler.handle(SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC, SOME_RECIPIENT_NAME);

        // then
        var expected = result.toBuilder()
                .id(1L)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .recipientName(SOME_RECIPIENT_NAME)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(expected);

        then(walletRepository).should().save(reservedWallet);
        then(claimTokenRepository).should().save(claimTokenCaptor.capture());
        var savedClaimToken = claimTokenCaptor.getValue();
        var expectedClaimToken = ClaimToken.builder()
                .remittanceId(result.remittanceId())
                .token(savedClaimToken.token())
                .claimed(false)
                .expiresAt(savedClaimToken.expiresAt())
                .build();
        assertThat(savedClaimToken)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expectedClaimToken);
        assertThat(savedClaimToken.expiresAt()).isNotNull();

        then(remittanceStatusEventRepository).should().save(argThat(event ->
                event.remittanceId().equals(result.remittanceId())
                        && event.status() == RemittanceStatus.INITIATED
                        && event.message().equals("Payment received")
                        && event.createdAt() != null
        ));

        then(workflowStarter).should().startWorkflow(
                result.remittanceId(),
                wallet.solanaAddress(),
                SOME_RECIPIENT_PHONE,
                SOME_AMOUNT_USDC,
                SOME_AMOUNT_INR,
                savedClaimToken.token());
    }

    @Test
    void shouldPropagateExceptionWhenWorkflowStarterFails() {
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

        given(remittanceRepository.save(argThat(r -> r != null && r.senderId() != null)))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0).toBuilder().id(1L).build());

        given(claimTokenRepository.save(argThat(ct -> ct != null && !ct.claimed())))
                .willAnswer(invocation -> invocation.getArgument(0));

        willThrow(new RuntimeException("Temporal unavailable"))
                .given(workflowStarter).startWorkflow(
                        argThat(id -> id != null),
                        argThat(addr -> addr != null),
                        argThat(phone -> phone != null),
                        argThat(amt -> amt != null),
                        argThat(inr -> inr != null),
                        argThat(tok -> tok != null));

        // when / then
        assertThatThrownBy(() -> createRemittanceHandler.handle(
                SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC, SOME_RECIPIENT_NAME))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Temporal unavailable");
    }

    @Test
    void shouldCreateRemittanceWithoutWorkflowStarterWhenNotConfigured() {
        // given
        var handler = new CreateRemittanceHandler(
                remittanceRepository, walletRepository, fxRateProvider,
                claimTokenRepository, Optional.empty(), remittanceStatusEventRepository);

        var wallet = walletBuilder()
                .availableBalance(BigDecimal.valueOf(500))
                .totalBalance(BigDecimal.valueOf(500))
                .build();

        var fxQuote = fxQuoteBuilder().build();

        given(walletRepository.findByUserId(SOME_SENDER_ID)).willReturn(Optional.of(wallet));

        var reservedWallet = wallet.reserveBalance(SOME_AMOUNT_USDC);
        given(walletRepository.save(reservedWallet)).willReturn(reservedWallet);

        given(fxRateProvider.getRate("USD", "INR")).willReturn(fxQuote);

        given(remittanceRepository.save(argThat(r -> r != null && r.senderId() != null)))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0).toBuilder().id(1L).build());

        given(claimTokenRepository.save(argThat(ct -> ct != null && !ct.claimed())))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = handler.handle(SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC, SOME_RECIPIENT_NAME);

        // then
        var expected = result.toBuilder()
                .senderId(SOME_SENDER_ID)
                .status(RemittanceStatus.INITIATED)
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expected);

        then(workflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // given
        given(walletRepository.findByUserId(SOME_SENDER_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> createRemittanceHandler.handle(
                SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC, SOME_RECIPIENT_NAME))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006");

        then(remittanceStatusEventRepository).shouldHaveNoInteractions();
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
                SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC, SOME_RECIPIENT_NAME))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("SP-0002");

        then(remittanceStatusEventRepository).shouldHaveNoInteractions();
    }
}
