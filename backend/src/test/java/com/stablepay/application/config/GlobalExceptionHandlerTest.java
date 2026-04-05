package com.stablepay.application.config;

import static com.stablepay.testutil.TestClockConfig.FIXED_INSTANT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.stablepay.domain.claim.exception.ClaimAlreadyClaimedException;
import com.stablepay.domain.claim.exception.ClaimTokenExpiredException;
import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.fx.exception.UnsupportedCorridorException;
import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.wallet.exception.InsufficientBalanceException;
import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;

import lombok.SneakyThrows;

class GlobalExceptionHandlerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler(FIXED_CLOCK))
                .setMessageConverters(converter)
                .build();
    }

    @RestController
    static class StubController {

        @GetMapping("/test/wallet-not-found")
        public void walletNotFound() {
            throw WalletNotFoundException.byId(1L);
        }

        @GetMapping("/test/insufficient-balance")
        public void insufficientBalance() {
            throw InsufficientBalanceException.forAmount(
                    BigDecimal.valueOf(500), BigDecimal.valueOf(100));
        }

        @GetMapping("/test/treasury-depleted")
        public void treasuryDepleted() {
            throw TreasuryDepletedException.insufficientTreasury(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(50));
        }

        @GetMapping("/test/wallet-already-exists")
        public void walletAlreadyExists() {
            throw WalletAlreadyExistsException.forUserId("user-123");
        }

        @GetMapping("/test/unsupported-corridor")
        public void unsupportedCorridor() {
            throw UnsupportedCorridorException.forPair("USD", "EUR");
        }

        @GetMapping("/test/claim-token-not-found")
        public void claimTokenNotFound() {
            throw ClaimTokenNotFoundException.byToken("missing-token");
        }

        @GetMapping("/test/claim-already-claimed")
        public void claimAlreadyClaimed() {
            throw ClaimAlreadyClaimedException.forToken("already-claimed-token");
        }

        @GetMapping("/test/claim-token-expired")
        public void claimTokenExpired() {
            throw ClaimTokenExpiredException.forToken("expired-token");
        }

        @GetMapping("/test/invalid-remittance-state")
        public void invalidRemittanceState() {
            throw InvalidRemittanceStateException.forClaim(RemittanceStatus.CANCELLED);
        }

        @GetMapping("/test/remittance-not-found")
        public void remittanceNotFound() {
            throw RemittanceNotFoundException.byId(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"));
        }

        @PostMapping("/test/validation")
        public void validation(@Valid @RequestBody ValidationRequest request) {}

        record ValidationRequest(@NotBlank String name) {}
    }

    @Test
    @SneakyThrows
    void shouldReturn404WithEnrichedResponseForWalletNotFound() {
        // given — stub endpoint throws WalletNotFoundException

        // when / then
        mockMvc.perform(get("/test/wallet-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0006"))
                .andExpect(jsonPath("$.message").value("SP-0006: Wallet not found: 1"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/wallet-not-found"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WithEnrichedResponseForInsufficientBalance() {
        // given — stub endpoint throws InsufficientBalanceException

        // when / then
        mockMvc.perform(get("/test/insufficient-balance"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0002"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0002: Insufficient balance. Requested: 500, Available: 100"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/insufficient-balance"));
    }

    @Test
    @SneakyThrows
    void shouldReturn503WithEnrichedResponseForTreasuryDepleted() {
        // given — stub endpoint throws TreasuryDepletedException

        // when / then
        mockMvc.perform(get("/test/treasury-depleted"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SP-0007"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0007: Treasury depleted. Requested: 1000, Available: 50"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/treasury-depleted"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WithEnrichedResponseForWalletAlreadyExists() {
        // given — stub endpoint throws WalletAlreadyExistsException

        // when / then
        mockMvc.perform(get("/test/wallet-already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0008"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0008: Wallet already exists for userId: user-123"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/wallet-already-exists"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WithEnrichedResponseForUnsupportedCorridor() {
        // given — stub endpoint throws UnsupportedCorridorException

        // when / then
        mockMvc.perform(get("/test/unsupported-corridor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0009"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0009: Unsupported corridor: USD -> EUR"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/unsupported-corridor"));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WithEnrichedResponseForClaimTokenNotFound() {
        // given — stub endpoint throws ClaimTokenNotFoundException

        // when / then
        mockMvc.perform(get("/test/claim-token-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0011"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0011: Claim token not found: missing-token"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/claim-token-not-found"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WithEnrichedResponseForClaimAlreadyClaimed() {
        // given — stub endpoint throws ClaimAlreadyClaimedException

        // when / then
        mockMvc.perform(get("/test/claim-already-claimed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0012"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0012: Claim already submitted for token: already-claimed-token"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/claim-already-claimed"));
    }

    @Test
    @SneakyThrows
    void shouldReturn410WithEnrichedResponseForClaimTokenExpired() {
        // given — stub endpoint throws ClaimTokenExpiredException

        // when / then
        mockMvc.perform(get("/test/claim-token-expired"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("SP-0013"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0013: Claim token expired: expired-token"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/claim-token-expired"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WithEnrichedResponseForInvalidRemittanceState() {
        // given — stub endpoint throws InvalidRemittanceStateException

        // when / then
        mockMvc.perform(get("/test/invalid-remittance-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0014"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0014: Cannot claim remittance in status: CANCELLED"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/invalid-remittance-state"));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WithEnrichedResponseForRemittanceNotFound() {
        // given — stub endpoint throws RemittanceNotFoundException

        // when / then
        mockMvc.perform(get("/test/remittance-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0010"))
                .andExpect(jsonPath("$.message").value(
                        "SP-0010: Remittance not found: 11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/remittance-not-found"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WithEnrichedResponseForValidationError() {
        // given
        var body = "{\"name\": \"\"}";

        // when / then
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.path").value("/test/validation"));
    }
}
