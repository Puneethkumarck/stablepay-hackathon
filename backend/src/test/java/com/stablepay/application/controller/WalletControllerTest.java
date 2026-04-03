package com.stablepay.application.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.stablepay.application.mapper.WalletApiMapperImpl;
import com.stablepay.domain.exception.TreasuryDepletedException;
import com.stablepay.domain.exception.WalletAlreadyExistsException;
import com.stablepay.domain.exception.WalletNotFoundException;
import com.stablepay.domain.model.Wallet;
import com.stablepay.domain.port.inbound.WalletService;

@WebMvcTest(WalletController.class)
@Import(WalletApiMapperImpl.class)
class WalletControllerTest {

    private static final Long WALLET_ID = 1L;
    private static final String USER_ID = "user-42";
    private static final String SOLANA_ADDRESS = "SoLaNa1234567890AbCdEfGhIjKlMnOpQrStUvWx";
    private static final Instant CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-03T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    void shouldCreateWallet() throws Exception {
        // given
        var wallet = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();

        given(walletService.create(USER_ID)).willReturn(wallet);

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-42"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(WALLET_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.solanaAddress").value(SOLANA_ADDRESS))
                .andExpect(jsonPath("$.availableBalance").value(0))
                .andExpect(jsonPath("$.totalBalance").value(0));
    }

    @Test
    void shouldFundWallet() throws Exception {
        // given
        var fundedWallet = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.valueOf(50))
                .totalBalance(BigDecimal.valueOf(50))
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();

        given(walletService.fund(WALLET_ID, BigDecimal.valueOf(50))).willReturn(fundedWallet);

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(WALLET_ID))
                .andExpect(jsonPath("$.availableBalance").value(50))
                .andExpect(jsonPath("$.totalBalance").value(50));
    }

    @Test
    void shouldReturn404WhenWalletNotFound() throws Exception {
        // given
        given(walletService.fund(WALLET_ID, BigDecimal.valueOf(50)))
                .willThrow(WalletNotFoundException.byId(WALLET_ID));

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0006"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn400WhenUserIdBlank() throws Exception {
        // given -- blank userId in request body

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn503WhenTreasuryDepleted() throws Exception {
        // given
        given(walletService.fund(WALLET_ID, BigDecimal.valueOf(50)))
                .willThrow(TreasuryDepletedException.insufficientTreasury(
                        BigDecimal.valueOf(50), BigDecimal.valueOf(10)));

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SP-0007"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn409WhenWalletAlreadyExists() throws Exception {
        // given
        given(walletService.create(USER_ID))
                .willThrow(WalletAlreadyExistsException.forUserId(USER_ID));

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-42"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0008"))
                .andExpect(jsonPath("$.message").exists());
    }
}
