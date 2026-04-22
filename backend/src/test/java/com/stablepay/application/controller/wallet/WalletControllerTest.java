package com.stablepay.application.controller.wallet;

import static com.stablepay.testutil.SecurityTestBase.asUser;
import static com.stablepay.testutil.WalletFixtures.SOME_BALANCE;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.controller.wallet.mapper.WalletApiMapper;
import com.stablepay.application.controller.wallet.mapper.WalletApiMapperImpl;
import com.stablepay.application.dto.CreateWalletRequest;
import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.handler.CreateWalletHandler;
import com.stablepay.domain.wallet.handler.GetWalletQueryHandler;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(WalletController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class, WalletApiMapperImpl.class})
class WalletControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWalletHandler createWalletHandler;

    @MockitoBean
    private GetWalletQueryHandler getWalletQueryHandler;

    @MockitoSpyBean
    private WalletApiMapper walletApiMapper;

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
    void shouldAllowAnonymousWalletCreation() {
        // given
        var wallet = walletBuilder()
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        given(createWalletHandler.handle(SOME_USER_ID)).willReturn(wallet);

        var request = CreateWalletRequest.builder()
                .userId(SOME_USER_ID)
                .build();

        // when / then
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SOME_WALLET_ID))
                .andExpect(jsonPath("$.solanaAddress").value(SOME_SOLANA_ADDRESS));
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

    @Test
    @SneakyThrows
    void shouldGetMyWallet() {
        // given
        var wallet = walletBuilder().build();

        given(getWalletQueryHandler.handle(SOME_USER_ID)).willReturn(wallet);

        // when / then
        mockMvc.perform(get("/api/wallets/me")
                        .with(asUser(SOME_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SOME_WALLET_ID))
                .andExpect(jsonPath("$.solanaAddress").value(SOME_SOLANA_ADDRESS))
                .andExpect(jsonPath("$.availableBalance").value(SOME_BALANCE.intValue()))
                .andExpect(jsonPath("$.totalBalance").value(SOME_BALANCE.intValue()));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenMyWalletNotFound() {
        // given
        given(getWalletQueryHandler.handle(SOME_USER_ID))
                .willThrow(WalletNotFoundException.byUserId(SOME_USER_ID));

        // when / then
        mockMvc.perform(get("/api/wallets/me")
                        .with(asUser(SOME_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0006"));
    }

    @Test
    @SneakyThrows
    void shouldReturn401WhenNoBearer() {
        // given

        // when / then
        mockMvc.perform(get("/api/wallets/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SP-0040"));
    }
}
