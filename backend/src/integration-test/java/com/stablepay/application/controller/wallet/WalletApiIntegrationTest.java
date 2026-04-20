package com.stablepay.application.controller.wallet;

import static com.stablepay.testutil.WalletFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PEER_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PUBLIC_KEY;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.dto.CreateWalletRequest;
import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.port.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

    private UUID createUser() {
        var userId = UUID.randomUUID();
        userRepository.save(AppUser.builder().id(userId).email(userId + "@test.com").build());
        return userId;
    }

    @Nested
    class CreateWallet {

        @Test
        @SneakyThrows
        void shouldCreateWalletAndReturnCreatedResponse() {
            // given
            var userId = createUser();
            var solanaAddress = SOME_SOLANA_ADDRESS + System.nanoTime();
            var generatedKey = GeneratedKey.builder()
                    .solanaAddress(solanaAddress)
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
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
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.solanaAddress").value(solanaAddress))
                    .andExpect(jsonPath("$.availableBalance").value(0))
                    .andExpect(jsonPath("$.totalBalance").value(0))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @SneakyThrows
        void shouldReturn409WhenWalletAlreadyExistsForUser() {
            // given
            var userId = createUser();
            var generatedKey = GeneratedKey.builder()
                    .solanaAddress("addr-dup-" + System.nanoTime())
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
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
        void shouldReturn400WhenUserIdIsNull() {
            // given
            var request = CreateWalletRequest.builder().userId(null).build();

            // when
            var result = mockMvc.perform(post("/api/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SP-0003"));
        }
    }
}
