package com.stablepay.application.controller.wallet;

import static com.stablepay.testutil.WalletFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.SOME_UPDATED_AT;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    private static final BigDecimal SOME_FUND_AMOUNT = BigDecimal.valueOf(500);

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
        var wallet = walletBuilder()
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
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

        var request = CreateWalletRequest.builder()
                .userId(SOME_USER_ID)
                .build();

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
        var wallet = walletBuilder()
                .availableBalance(SOME_FUND_AMOUNT)
                .totalBalance(SOME_FUND_AMOUNT)
                .build();

        var response = WalletResponse.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(SOME_FUND_AMOUNT)
                .totalBalance(SOME_FUND_AMOUNT)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        given(fundWalletHandler.handle(SOME_WALLET_ID, SOME_FUND_AMOUNT))
                .willReturn(wallet);
        given(walletApiMapper.toResponse(wallet)).willReturn(response);

        var request = FundWalletRequest.builder().amount(SOME_FUND_AMOUNT).build();

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
        given(fundWalletHandler.handle(SOME_WALLET_ID, SOME_FUND_AMOUNT))
                .willThrow(WalletNotFoundException.byId(SOME_WALLET_ID));

        var request = FundWalletRequest.builder().amount(SOME_FUND_AMOUNT).build();

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
        given(fundWalletHandler.handle(SOME_WALLET_ID, SOME_FUND_AMOUNT))
                .willThrow(TreasuryDepletedException.insufficientTreasury(
                        SOME_FUND_AMOUNT, BigDecimal.valueOf(100)));

        var request = FundWalletRequest.builder().amount(SOME_FUND_AMOUNT).build();

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

        var request = CreateWalletRequest.builder()
                .userId(SOME_USER_ID)
                .build();

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SP-0008"));
    }
}
