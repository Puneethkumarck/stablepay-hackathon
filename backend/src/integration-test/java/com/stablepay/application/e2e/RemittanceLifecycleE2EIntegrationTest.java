package com.stablepay.application.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.stablepay.domain.remittance.handler.UpdateRemittanceStatusHandler;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.wallet.model.GeneratedKey;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.test.PgTest;

import lombok.SneakyThrows;

@PgTest
@AutoConfigureMockMvc
class RemittanceLifecycleE2EIntegrationTest {

    private static final String E2E_USER_ID = "e2e-user-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String E2E_SOLANA_ADDRESS = "E2eSoLaNa1234567890AbCdEfGhIjKlMnOpQrStUv";
    private static final byte[] E2E_PUBLIC_KEY = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    private static final byte[] E2E_KEY_SHARE_DATA = new byte[]{10, 20, 30, 40, 50};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MpcWalletClient mpcWalletClient;

    @Autowired
    private UpdateRemittanceStatusHandler updateRemittanceStatusHandler;

    @Autowired
    private WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        given(mpcWalletClient.generateKey()).willReturn(
                GeneratedKey.builder()
                        .solanaAddress(E2E_SOLANA_ADDRESS)
                        .publicKey(E2E_PUBLIC_KEY)
                        .keyShareData(E2E_KEY_SHARE_DATA)
                        .build());
    }

    @Test
    @SneakyThrows
    void shouldCompleteFullRemittanceLifecycle() {
        // Step 1: Create Wallet
        var walletId = createWallet();

        // Step 2: Fund Wallet (100 USDC)
        fundWallet(walletId);

        // Step 3: Check FX Rate
        checkFxRate();

        // Step 4: Create Remittance (25 USDC)
        var remittanceResult = createRemittance();
        var remittanceId = remittanceResult.remittanceId();
        var claimToken = remittanceResult.claimTokenId();

        // Step 5: Get Remittance — verify INITIATED
        getRemittance(remittanceId, "INITIATED");

        // Step 6: Advance to ESCROWED (simulates Temporal workflow deposit activity)
        updateRemittanceStatusHandler.handle(UUID.fromString(remittanceId), RemittanceStatus.ESCROWED);

        // Step 7: Get Claim Details
        getClaimDetails(claimToken, remittanceId);

        // Step 8: Submit Claim
        submitClaim(claimToken);

        // Step 9: Get Remittance — verify still ESCROWED (workflow not running in test)
        getRemittance(remittanceId, "ESCROWED");

        // Step 10: List Remittances — verify sender's remittance appears
        listRemittances();
    }

    @SneakyThrows
    private Long createWallet() {
        // given
        var body = """
                { "userId": "%s" }
                """.formatted(E2E_USER_ID);

        // when
        var result = mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(E2E_USER_ID))
                .andExpect(jsonPath("$.solanaAddress").value(E2E_SOLANA_ADDRESS))
                .andExpect(jsonPath("$.availableBalance").value(0))
                .andReturn();

        // then
        var json = result.getResponse().getContentAsString();
        Integer walletId = JsonPath.read(json, "$.id");
        assertThat(walletId).isPositive();
        return walletId.longValue();
    }

    private void fundWallet(Long walletId) {
        var wallet = walletRepository.findBySolanaAddress(E2E_SOLANA_ADDRESS).orElseThrow();
        assertThat(wallet.id()).isEqualTo(walletId);
        walletRepository.save(wallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(100))
                .totalBalance(BigDecimal.valueOf(100))
                .build());
    }

    @SneakyThrows
    private void checkFxRate() {
        // when / then
        mockMvc.perform(get("/api/fx/USD-INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").isNumber())
                .andExpect(jsonPath("$.source").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @SneakyThrows
    private RemittanceResult createRemittance() {
        // given
        var body = """
                {
                    "senderId": "%s",
                    "recipientPhone": "+919876543210",
                    "amountUsdc": 25.00
                }
                """.formatted(E2E_USER_ID);

        // when
        var result = mockMvc.perform(post("/api/remittances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remittanceId").isString())
                .andExpect(jsonPath("$.senderId").value(E2E_USER_ID))
                .andExpect(jsonPath("$.recipientPhone").value("+919876543210"))
                .andExpect(jsonPath("$.amountUsdc").value(25.00))
                .andExpect(jsonPath("$.amountInr").isNumber())
                .andExpect(jsonPath("$.fxRate").isNumber())
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.claimTokenId").isString())
                .andReturn();

        // then
        var json = result.getResponse().getContentAsString();
        var remittanceId = (String) JsonPath.read(json, "$.remittanceId");
        var claimTokenId = (String) JsonPath.read(json, "$.claimTokenId");
        assertThat(remittanceId).isNotBlank();
        assertThat(claimTokenId).isNotBlank();
        return new RemittanceResult(remittanceId, claimTokenId);
    }

    @SneakyThrows
    private void getRemittance(String remittanceId, String expectedStatus) {
        // when / then
        mockMvc.perform(get("/api/remittances/{remittanceId}", remittanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remittanceId").value(remittanceId))
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    @SneakyThrows
    private void getClaimDetails(String claimToken, String remittanceId) {
        // when / then
        mockMvc.perform(get("/api/claims/{token}", claimToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remittanceId").value(remittanceId))
                .andExpect(jsonPath("$.amountUsdc").value(25.00))
                .andExpect(jsonPath("$.amountInr").isNumber())
                .andExpect(jsonPath("$.claimed").value(false))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @SneakyThrows
    private void submitClaim(String claimToken) {
        // given
        var body = """
                { "upiId": "recipient@upi" }
                """;

        // when / then
        mockMvc.perform(post("/api/claims/{token}", claimToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(true));
    }

    @SneakyThrows
    private void listRemittances() {
        // when / then
        mockMvc.perform(get("/api/remittances")
                        .param("senderId", E2E_USER_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].senderId").value(E2E_USER_ID))
                .andExpect(jsonPath("$.content[0].amountUsdc").value(25.00))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private record RemittanceResult(String remittanceId, String claimTokenId) {}
}
