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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.config.SecurityAuthenticationEntryPoint;
import com.stablepay.application.controller.wallet.mapper.WalletApiMapper;
import com.stablepay.application.dto.CreateWalletRequest;
import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.handler.CreateWalletHandler;
import com.stablepay.testutil.TestClockConfig;

import lombok.SneakyThrows;

@WebMvcTest(WalletController.class)
@Import(TestClockConfig.class)
class WalletControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWalletHandler createWalletHandler;

    @MockitoBean
    private WalletApiMapper walletApiMapper;

    @MockitoBean
    private SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

    @Test
    @SneakyThrows
    void shouldCreateWallet() {
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
                .andExpect(jsonPath("$.userId").value(SOME_USER_ID.toString()))
                .andExpect(jsonPath("$.solanaAddress").value(SOME_SOLANA_ADDRESS));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenValidationFails() {
        // given
        var request = CreateWalletRequest.builder().userId(null).build();

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }

    @Test
    @SneakyThrows
    void shouldReturn409WhenWalletAlreadyExists() {
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
