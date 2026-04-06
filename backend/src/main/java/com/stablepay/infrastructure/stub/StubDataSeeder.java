package com.stablepay.infrastructure.stub;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.stablepay.domain.claim.model.ClaimToken;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("stub")
@RequiredArgsConstructor
public class StubDataSeeder implements ApplicationRunner {

    static final String DEMO_USER_ID = "demo-user-1";
    static final String DEMO_SOLANA_ADDRESS = "DemoSoLAddress1234567890AbCdEfGhIjKlMnOpQr";
    static final UUID DEMO_REMITTANCE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    static final String DEMO_CLAIM_TOKEN = "demo-claim-token-abc123";
    static final String DEMO_RECIPIENT_PHONE = "+919876543210";
    static final BigDecimal DEMO_BALANCE = new BigDecimal("500.000000");
    static final BigDecimal DEMO_AMOUNT_USDC = new BigDecimal("100.000000");
    static final BigDecimal DEMO_FX_RATE = new BigDecimal("83.250000");
    static final BigDecimal DEMO_AMOUNT_INR = new BigDecimal("8325.00");

    private final WalletRepository walletRepository;
    private final RemittanceRepository remittanceRepository;
    private final ClaimTokenRepository claimTokenRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding stub demo data for frontend development...");

        var wallet = seedWallet();
        var remittance = seedRemittance();
        seedClaimToken(remittance);

        log.info("Stub data seeding complete. Demo wallet id={}, remittance id={}",
                wallet.id(), remittance.remittanceId());
    }

    private Wallet seedWallet() {
        var wallet = Wallet.builder()
                .userId(DEMO_USER_ID)
                .solanaAddress(DEMO_SOLANA_ADDRESS)
                .availableBalance(DEMO_BALANCE)
                .totalBalance(DEMO_BALANCE)
                .build();
        var saved = walletRepository.save(wallet);
        log.info("Seeded demo wallet: userId={}, solanaAddress={}, balance={}",
                saved.userId(), saved.solanaAddress(), saved.availableBalance());
        return saved;
    }

    private Remittance seedRemittance() {
        var now = Instant.now();
        var remittance = Remittance.builder()
                .remittanceId(DEMO_REMITTANCE_ID)
                .senderId(DEMO_USER_ID)
                .recipientPhone(DEMO_RECIPIENT_PHONE)
                .amountUsdc(DEMO_AMOUNT_USDC)
                .amountInr(DEMO_AMOUNT_INR)
                .fxRate(DEMO_FX_RATE)
                .status(RemittanceStatus.ESCROWED)
                .smsNotificationFailed(false)
                .expiresAt(now.plusSeconds(172800))
                .build();
        var saved = remittanceRepository.save(remittance);
        log.info("Seeded demo remittance: id={}, status={}, amount={} USDC",
                saved.remittanceId(), saved.status(), saved.amountUsdc());
        return saved;
    }

    private void seedClaimToken(Remittance remittance) {
        var claimToken = ClaimToken.builder()
                .remittanceId(remittance.remittanceId())
                .token(DEMO_CLAIM_TOKEN)
                .claimed(false)
                .expiresAt(Instant.now().plusSeconds(172800))
                .build();
        var saved = claimTokenRepository.save(claimToken);
        log.info("Seeded demo claim token: token={}, remittanceId={}",
                saved.token(), saved.remittanceId());
    }
}
