package com.stablepay.domain.remittance.handler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.claim.model.ClaimToken;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.fx.model.FxQuote;
import com.stablepay.domain.fx.port.FxRateProvider;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.domain.remittance.port.RemittanceWorkflowStarter;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateRemittanceHandler {

    private static final Duration CLAIM_EXPIRY = Duration.ofHours(48);

    private final RemittanceRepository remittanceRepository;
    private final WalletRepository walletRepository;
    private final FxRateProvider fxRateProvider;
    private final ClaimTokenRepository claimTokenRepository;
    private final Optional<RemittanceWorkflowStarter> workflowStarter;

    public Remittance handle(UUID senderId, String recipientPhone, BigDecimal amountUsdc) {
        var wallet = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> WalletNotFoundException.byUserId(senderId));

        var reserved = wallet.reserveBalance(amountUsdc);
        walletRepository.save(reserved);

        var fxQuote = fxRateProvider.getRate("USD", "INR");
        var amountInr = calculateInrAmount(amountUsdc, fxQuote);

        var remittance = Remittance.builder()
                .remittanceId(UUID.randomUUID())
                .senderId(senderId)
                .recipientPhone(recipientPhone)
                .amountUsdc(amountUsdc)
                .amountInr(amountInr)
                .fxRate(fxQuote.rate())
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .build();

        var saved = remittanceRepository.save(remittance);

        var claimToken = ClaimToken.builder()
                .remittanceId(saved.remittanceId())
                .token(UUID.randomUUID().toString())
                .claimed(false)
                .expiresAt(Instant.now().plus(CLAIM_EXPIRY))
                .build();
        var savedClaim = claimTokenRepository.save(claimToken);

        var finalRemittance = remittanceRepository.save(
                saved.toBuilder().claimTokenId(savedClaim.token()).build());

        log.info("Created remittance remittanceId={}, senderId={}, amountUsdc={}, fxRate={}",
                finalRemittance.remittanceId(), senderId, amountUsdc, fxQuote.rate());

        workflowStarter.ifPresent(starter -> starter.startWorkflow(
                finalRemittance.remittanceId(),
                wallet.solanaAddress(),
                recipientPhone,
                amountUsdc,
                amountInr,
                savedClaim.token()));

        return finalRemittance;
    }

    private BigDecimal calculateInrAmount(BigDecimal amountUsdc, FxQuote fxQuote) {
        return amountUsdc.multiply(fxQuote.rate()).setScale(2, RoundingMode.HALF_UP);
    }
}
