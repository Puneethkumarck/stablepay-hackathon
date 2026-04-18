package com.stablepay.infrastructure.temporal;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.funding.handler.FinalizeFundingHandler;
import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.port.TreasuryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletFundingActivitiesImpl implements WalletFundingActivities {

    private static final BigDecimal MIN_SENDER_SOL = new BigDecimal("0.005");
    private static final long SOL_TOP_UP_LAMPORTS = 10_000_000L;

    private final TreasuryService treasuryService;
    private final FinalizeFundingHandler finalizeFundingHandler;

    @Override
    public void checkTreasuryBalance(BigDecimal amountUsdc) {
        requirePositiveAmount(amountUsdc);
        var available = treasuryService.getTreasuryUsdcBalance();
        if (available.compareTo(amountUsdc) < 0) {
            log.error("Treasury USDC insufficient requested={} available={}", amountUsdc, available);
            throw TreasuryDepletedException.insufficientTreasury(amountUsdc, available);
        }
        log.info("Treasury USDC sufficient requested={} available={}", amountUsdc, available);
    }

    @Override
    public void ensureSolBalance(String senderSolanaAddress) {
        requireNonNull(senderSolanaAddress, "senderSolanaAddress cannot be null");
        var senderSol = treasuryService.getSolBalance(senderSolanaAddress);
        if (senderSol.compareTo(MIN_SENDER_SOL) >= 0) {
            log.debug("Sender {} SOL {} >= {} — no top-up",
                    senderSolanaAddress, senderSol, MIN_SENDER_SOL);
            return;
        }
        log.info("Topping up sender {} with {} lamports (current {} SOL)",
                senderSolanaAddress, SOL_TOP_UP_LAMPORTS, senderSol);
        treasuryService.transferSol(senderSolanaAddress, SOL_TOP_UP_LAMPORTS);
    }

    @Override
    public void createAtaIfNeeded(String senderSolanaAddress) {
        requireNonNull(senderSolanaAddress, "senderSolanaAddress cannot be null");
        treasuryService.createAtaIfNeeded(senderSolanaAddress);
    }

    @Override
    public String transferUsdc(String senderSolanaAddress, BigDecimal amountUsdc) {
        requireNonNull(senderSolanaAddress, "senderSolanaAddress cannot be null");
        requirePositiveAmount(amountUsdc);
        return treasuryService.transferUsdc(senderSolanaAddress, amountUsdc);
    }

    @Override
    public void finalizeFunding(UUID fundingId, Long walletId, BigDecimal amountUsdc) {
        requireNonNull(fundingId, "fundingId cannot be null");
        requireNonNull(walletId, "walletId cannot be null");
        requirePositiveAmount(amountUsdc);
        finalizeFundingHandler.handle(fundingId, walletId, amountUsdc);
    }

    private static void requirePositiveAmount(BigDecimal amountUsdc) {
        requireNonNull(amountUsdc, "amountUsdc cannot be null");
        if (amountUsdc.signum() <= 0) {
            throw new IllegalArgumentException(
                    "amountUsdc must be positive, got: " + amountUsdc);
        }
    }
}
