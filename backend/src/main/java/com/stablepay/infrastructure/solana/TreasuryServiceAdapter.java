package com.stablepay.infrastructure.solana;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.stablepay.domain.port.outbound.TreasuryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TreasuryServiceAdapter implements TreasuryService {

    private static final BigDecimal HARDCODED_BALANCE = BigDecimal.valueOf(1_000_000);

    @Override
    public BigDecimal getBalance() {
        log.info("Querying treasury balance (hardcoded): {}", HARDCODED_BALANCE);
        return HARDCODED_BALANCE;
    }

    @Override
    public void transferFromTreasury(String destinationAddress, BigDecimal amount) {
        log.info("Treasury transfer: {} USDC to {}", amount, destinationAddress);
    }
}
