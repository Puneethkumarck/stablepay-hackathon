package com.stablepay.infrastructure.stub;

import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_AMOUNT_INR;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_AMOUNT_USDC;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_BALANCE;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_CLAIM_TOKEN;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_FX_RATE;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_RECIPIENT_PHONE;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_REMITTANCE_ID;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_SOLANA_ADDRESS;
import static com.stablepay.infrastructure.stub.StubDataSeeder.DEMO_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import com.stablepay.domain.claim.model.ClaimToken;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class StubDataSeederTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private RemittanceRepository remittanceRepository;

    @Mock
    private ClaimTokenRepository claimTokenRepository;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private StubDataSeeder seeder;

    @Captor
    private ArgumentCaptor<Wallet> walletCaptor;

    @Captor
    private ArgumentCaptor<Remittance> remittanceCaptor;

    @Captor
    private ArgumentCaptor<ClaimToken> claimTokenCaptor;

    @Test
    void shouldSeedDemoData() {
        // given
        var savedWallet = Wallet.builder()
                .id(1L)
                .userId(DEMO_USER_ID)
                .solanaAddress(DEMO_SOLANA_ADDRESS)
                .availableBalance(DEMO_BALANCE)
                .totalBalance(DEMO_BALANCE)
                .build();

        var savedRemittance = Remittance.builder()
                .id(1L)
                .remittanceId(DEMO_REMITTANCE_ID)
                .senderId(DEMO_USER_ID)
                .recipientPhone(DEMO_RECIPIENT_PHONE)
                .amountUsdc(DEMO_AMOUNT_USDC)
                .amountInr(DEMO_AMOUNT_INR)
                .fxRate(DEMO_FX_RATE)
                .status(RemittanceStatus.ESCROWED)
                .smsNotificationFailed(false)
                .build();

        var savedClaimToken = ClaimToken.builder()
                .id(1L)
                .remittanceId(DEMO_REMITTANCE_ID)
                .token(DEMO_CLAIM_TOKEN)
                .claimed(false)
                .build();

        given(walletRepository.save(walletCaptor.capture())).willReturn(savedWallet);
        given(remittanceRepository.save(remittanceCaptor.capture())).willReturn(savedRemittance);
        given(claimTokenRepository.save(claimTokenCaptor.capture())).willReturn(savedClaimToken);

        // when
        seeder.run(applicationArguments);

        // then
        then(walletRepository).should().save(walletCaptor.getValue());
        then(remittanceRepository).should().save(remittanceCaptor.getValue());
        then(claimTokenRepository).should().save(claimTokenCaptor.getValue());

        var capturedWallet = walletCaptor.getValue();
        assertThat(capturedWallet.userId()).isEqualTo(DEMO_USER_ID);
        assertThat(capturedWallet.solanaAddress()).isEqualTo(DEMO_SOLANA_ADDRESS);
        assertThat(capturedWallet.availableBalance()).isEqualByComparingTo(DEMO_BALANCE);

        var capturedRemittance = remittanceCaptor.getValue();
        assertThat(capturedRemittance.remittanceId()).isEqualTo(DEMO_REMITTANCE_ID);
        assertThat(capturedRemittance.status()).isEqualTo(RemittanceStatus.ESCROWED);
        assertThat(capturedRemittance.amountUsdc()).isEqualByComparingTo(DEMO_AMOUNT_USDC);

        var capturedClaimToken = claimTokenCaptor.getValue();
        assertThat(capturedClaimToken.token()).isEqualTo(DEMO_CLAIM_TOKEN);
        assertThat(capturedClaimToken.claimed()).isFalse();
    }
}
