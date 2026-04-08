package com.stablepay.application.controller.wallet;

import static com.stablepay.testutil.WalletFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PUBLIC_KEY;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.dto.CreateWalletRequest;
import com.stablepay.application.dto.FundWalletRequest;
import com.stablepay.domain.wallet.model.GeneratedKey;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.test.PgTest;

import lombok.SneakyThrows;

@PgTest
@AutoConfigureMockMvc
class WalletApiIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MpcWalletClient mpcWalletClient;

    @Nested
    class CreateWallet {

        @Test
        @SneakyThrows
        void shouldCreateWalletAndReturnCreatedResponse() {
            // given
            var userId = "api-user-" + System.nanoTime();
            var solanaAddress = SOME_SOLANA_ADDRESS + System.nanoTime();
            var generatedKey = GeneratedKey.builder()
                    .solanaAddress(solanaAddress)
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .build();
            given(mpcWalletClient.generateKey()).willReturn(generatedKey);

            var request = CreateWalletRequest.builder().userId(userId).build();

            // when
            var result = mockMvc.perform(post("/api/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.solanaAddress").value(solanaAddress))
                    .andExpect(jsonPath("$.availableBalance").value(0))
                    .andExpect(jsonPath("$.totalBalance").value(0))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @SneakyThrows
        void shouldReturn409WhenWalletAlreadyExistsForUser() {
            // given
            var userId = "dup-user-" + System.nanoTime();
            var generatedKey = GeneratedKey.builder()
                    .solanaAddress("addr-dup-" + System.nanoTime())
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .build();
            given(mpcWalletClient.generateKey()).willReturn(generatedKey);

            var request = CreateWalletRequest.builder().userId(userId).build();
            var body = OBJECT_MAPPER.writeValueAsString(request);

            mockMvc.perform(post("/api/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated());

            // when — second create with same userId
            var result = mockMvc.perform(post("/api/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("SP-0008"));
        }

        @Test
        @SneakyThrows
        void shouldReturn400WhenUserIdIsBlank() {
            // given
            var request = CreateWalletRequest.builder().userId("").build();

            // when
            var result = mockMvc.perform(post("/api/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SP-0003"));
        }
    }

    @Nested
    class FundWallet {

        @Test
        @SneakyThrows
        void shouldFundWalletAndReturnUpdatedBalance() {
            // given
            var userId = "fund-user-" + System.nanoTime();
            var generatedKey = GeneratedKey.builder()
                    .solanaAddress("addr-fund-" + System.nanoTime())
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .build();
            given(mpcWalletClient.generateKey()).willReturn(generatedKey);

            var createRequest = CreateWalletRequest.builder().userId(userId).build();
            var createResult = mockMvc.perform(post("/api/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            var walletId = OBJECT_MAPPER.readTree(
                    createResult.getResponse().getContentAsString()).get("id").asLong();
            var fundAmount = new BigDecimal("500.00");
            var fundRequest = FundWalletRequest.builder().amount(fundAmount).build();

            // when
            var result = mockMvc.perform(post("/api/wallets/{id}/fund", walletId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(fundRequest)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(walletId))
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.availableBalance").value(500.0))
                    .andExpect(jsonPath("$.totalBalance").value(500.0));
        }

        @Test
        @SneakyThrows
        void shouldReturn404WhenFundingNonexistentWallet() {
            // given
            var fundRequest = FundWalletRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .build();

            // when
            var result = mockMvc.perform(post("/api/wallets/{id}/fund", 999999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(fundRequest)));

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("SP-0006"));
        }
    }
}
