package com.stablepay.application.controller.auth;

import static com.stablepay.testutil.AuthFixtures.SOME_ID_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_PROVIDER;
import static com.stablepay.testutil.WalletFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PEER_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PUBLIC_KEY;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.dto.RefreshTokenRequest;
import com.stablepay.application.dto.SocialLoginRequest;
import com.stablepay.domain.auth.exception.InvalidIdTokenException;
import com.stablepay.domain.auth.model.AuthPrincipal;
import com.stablepay.domain.auth.port.SocialIdentityVerifier;
import com.stablepay.domain.wallet.model.GeneratedKey;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.test.PgTest;
import com.stablepay.testutil.AuthFixtures;

import lombok.SneakyThrows;

@PgTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialIdentityVerifier socialIdentityVerifier;

    @Autowired
    private MpcWalletClient mpcWalletClient;

    @Nested
    class SocialLogin {

        @Test
        @SneakyThrows
        void shouldReturn201ForNewUser() {
            // given
            var identity = AuthFixtures.socialIdentityBuilder().build();
            given(socialIdentityVerifier.verify(SOME_PROVIDER, SOME_ID_TOKEN)).willReturn(identity);

            var generatedKey = GeneratedKey.builder()
                    .solanaAddress("addr-auth-" + System.nanoTime())
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                    .build();
            given(mpcWalletClient.generateKey()).willReturn(generatedKey);

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/social")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(jsonPath("$.user.id").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value(identity.email()))
                    .andExpect(jsonPath("$.wallet.solanaAddress").value(generatedKey.solanaAddress()));
        }

        @Test
        @SneakyThrows
        void shouldReturn200ForReturningUser() {
            // given
            var identity = AuthFixtures.socialIdentityBuilder()
                    .email("returning-" + System.nanoTime() + "@gmail.com")
                    .subject("google-sub-returning-" + System.nanoTime())
                    .build();
            var idToken = "returning-id-token-" + System.nanoTime();
            given(socialIdentityVerifier.verify(SOME_PROVIDER, idToken)).willReturn(identity);

            var generatedKey = GeneratedKey.builder()
                    .solanaAddress("addr-return-" + System.nanoTime())
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                    .build();
            given(mpcWalletClient.generateKey()).willReturn(generatedKey);

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(idToken)
                    .build();

            mockMvc.perform(post("/api/auth/social")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // when
            var result = mockMvc.perform(post("/api/auth/social")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value(identity.email()));
        }

        @Test
        @SneakyThrows
        void shouldReturn401WhenIdTokenInvalid() {
            // given
            given(socialIdentityVerifier.verify(SOME_PROVIDER, "bad-token"))
                    .willThrow(InvalidIdTokenException.of("bad signature"));

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken("bad-token")
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/social")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("SP-0032"));
        }
    }

    @Nested
    class RefreshToken {

        @Test
        @SneakyThrows
        void shouldReturn200WithNewTokensOnRefresh() {
            // given
            var identity = AuthFixtures.socialIdentityBuilder()
                    .email("refresh-" + System.nanoTime() + "@gmail.com")
                    .subject("google-sub-refresh-" + System.nanoTime())
                    .build();
            var idToken = "refresh-id-token-" + System.nanoTime();
            given(socialIdentityVerifier.verify(SOME_PROVIDER, idToken)).willReturn(identity);

            var generatedKey = GeneratedKey.builder()
                    .solanaAddress("addr-refresh-" + System.nanoTime())
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                    .build();
            given(mpcWalletClient.generateKey()).willReturn(generatedKey);

            var loginRequest = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(idToken)
                    .build();

            var loginResponse = mockMvc.perform(post("/api/auth/social")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(loginRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            var loginBody = OBJECT_MAPPER.readTree(loginResponse.getResponse().getContentAsString());
            var refreshToken = loginBody.get("refreshToken").asText();

            var refreshRequest = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(refreshRequest)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(jsonPath("$.user").doesNotExist())
                    .andExpect(jsonPath("$.wallet").doesNotExist());
        }
    }

    @Nested
    class Logout {

        @Test
        @SneakyThrows
        void shouldReturn204OnLogout() {
            // given
            var identity = AuthFixtures.socialIdentityBuilder()
                    .email("logout-" + System.nanoTime() + "@gmail.com")
                    .subject("google-sub-logout-" + System.nanoTime())
                    .build();
            var idToken = "logout-id-token-" + System.nanoTime();
            given(socialIdentityVerifier.verify(SOME_PROVIDER, idToken)).willReturn(identity);

            var generatedKey = GeneratedKey.builder()
                    .solanaAddress("addr-logout-" + System.nanoTime())
                    .publicKey(SOME_PUBLIC_KEY)
                    .keyShareData(SOME_KEY_SHARE_DATA)
                    .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                    .build();
            given(mpcWalletClient.generateKey()).willReturn(generatedKey);

            var loginRequest = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(idToken)
                    .build();

            var loginResponse = mockMvc.perform(post("/api/auth/social")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(loginRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            var loginBody = OBJECT_MAPPER.readTree(loginResponse.getResponse().getContentAsString());
            var userId = UUID.fromString(loginBody.get("user").get("id").asText());
            var principal = AuthPrincipal.builder().id(userId).build();
            var jwt = Jwt.withTokenValue("test-token")
                    .header("alg", "HS256")
                    .subject(userId.toString())
                    .build();
            var authentication = new JwtAuthenticationToken(jwt, Collections.emptyList(), userId.toString()) {
                @Override
                public Object getPrincipal() {
                    return principal;
                }
            };

            // when
            var result = mockMvc.perform(post("/api/auth/logout")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)));

            // then
            result.andExpect(status().isNoContent());
        }

        @Test
        @SneakyThrows
        void shouldReturn401WhenNotAuthenticated() {
            // given

            // when
            var result = mockMvc.perform(post("/api/auth/logout"));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }
}
