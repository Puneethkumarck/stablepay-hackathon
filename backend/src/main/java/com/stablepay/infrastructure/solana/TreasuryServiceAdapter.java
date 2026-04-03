package com.stablepay.infrastructure.solana;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.stablepay.domain.wallet.port.TreasuryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TreasuryServiceAdapter implements TreasuryService {

    @Override
    public void transferFromTreasury(String destinationAddress, BigDecimal amount) {
        log.info("STUB: Transferring {} USDC from treasury to {}", amount, destinationAddress);
    }

    @Override
    public BigDecimal getBalance() {
        log.debug("STUB: Returning treasury balance of 1,000,000 USDC");
        return BigDecimal.valueOf(1_000_000);
    }
}
