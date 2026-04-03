package com.stablepay.application.controller.wallet;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.controller.wallet.mapper.WalletApiMapper;
import com.stablepay.application.dto.CreateWalletRequest;
import com.stablepay.application.dto.FundWalletRequest;
import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.handler.CreateWalletHandler;
import com.stablepay.domain.wallet.handler.FundWalletHandler;
import com.stablepay.domain.wallet.model.Wallet;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    private static final String SOME_USER_ID = "user-123";
    private static final String SOME_SOLANA_ADDRESS = "SoLaNaAdDrEsS123456789";
    private static final Long SOME_WALLET_ID = 1L;
    private static final Instant SOME_CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");
    private static final Instant SOME_UPDATED_AT = Instant.parse("2026-04-03T10:00:00Z");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWalletHandler createWalletHandler;

    @MockitoBean
    private FundWalletHandler fundWalletHandler;

    @MockitoBean
    private WalletApiMapper walletApiMapper;

    @Test
    void shouldCreateWallet() throws Exception {
        // given
        var wallet = Wallet.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        var response = WalletResponse.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        given(createWalletHandler.handle(SOME_USER_ID)).willReturn(wallet);
        given(walletApiMapper.toResponse(wallet)).willReturn(response);

        var request = CreateWalletRequest.builder().userId(SOME_USER_ID).build();

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SOME_WALLET_ID))
                .andExpect(jsonPath("$.userId").value(SOME_USER_ID))
                .andExpect(jsonPath("$.solanaAddress").value(SOME_SOLANA_ADDRESS));
    }

    @Test
    void shouldFundWallet() throws Exception {
        // given
        var fundAmount = BigDecimal.valueOf(500);

        var wallet = Wallet.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(fundAmount)
                .totalBalance(fundAmount)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        var response = WalletResponse.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(fundAmount)
                .totalBalance(fundAmount)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        given(fundWalletHandler.handle(SOME_WALLET_ID, fundAmount)).willReturn(wallet);
        given(walletApiMapper.toResponse(wallet)).willReturn(response);

        var request = FundWalletRequest.builder().amount(fundAmount).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SOME_WALLET_ID))
                .andExpect(jsonPath("$.availableBalance").value(500));
    }

    @Test
    void shouldReturn404WhenWalletNotFound() throws Exception {
        // given
        var fundAmount = BigDecimal.valueOf(500);
        given(fundWalletHandler.handle(SOME_WALLET_ID, fundAmount))
                .willThrow(WalletNotFoundException.byId(SOME_WALLET_ID));

        var request = FundWalletRequest.builder().amount(fundAmount).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0006"));
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        // given
        var request = CreateWalletRequest.builder().userId("").build();

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }

    @Test
    void shouldReturn503WhenTreasuryDepleted() throws Exception {
        // given
        var fundAmount = BigDecimal.valueOf(500);
        given(fundWalletHandler.handle(SOME_WALLET_ID, fundAmount))
                .willThrow(TreasuryDepletedException.insufficientTreasury(
                        fundAmount, BigDecimal.valueOf(100)));

        var request = FundWalletRequest.builder().amount(fundAmount).build();

        // when / then
        mockMvc.perform(post("/api/wallets/{id}/fund", SOME_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SP-0007"));
    }

    @Test
    void shouldReturn409WhenWalletAlreadyExists() throws Exception {
        // given
        given(createWalletHandler.handle(SOME_USER_ID))
                .willThrow(WalletAlreadyExistsException.forUserId(SOME_USER_ID));

        var request = CreateWalletRequest.builder().userId(SOME_USER_ID).build();

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0008"));
    }
}
