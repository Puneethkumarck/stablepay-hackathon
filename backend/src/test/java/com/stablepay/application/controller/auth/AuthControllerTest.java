package com.stablepay.application.controller.auth;

import static com.stablepay.testutil.AuthFixtures.SOME_ACCESS_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_ID_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_PROVIDER;
import static com.stablepay.testutil.AuthFixtures.SOME_RAW_REFRESH_TOKEN;
import static com.stablepay.testutil.AuthFixtures.authSessionBuilder;
import static com.stablepay.testutil.SecurityTestBase.asUser;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.controller.auth.mapper.AuthResponseMapper;
import com.stablepay.application.controller.auth.mapper.AuthResponseMapperImpl;
import com.stablepay.application.dto.RefreshTokenRequest;
import com.stablepay.application.dto.SocialLoginRequest;
import com.stablepay.domain.auth.exception.InvalidIdTokenException;
import com.stablepay.domain.auth.exception.InvalidRefreshTokenException;
import com.stablepay.domain.auth.exception.UnsupportedAuthProviderException;
import com.stablepay.domain.auth.handler.LogoutHandler;
import com.stablepay.domain.auth.handler.RefreshTokenHandler;
import com.stablepay.domain.auth.handler.SocialLoginHandler;
import com.stablepay.domain.auth.model.AuthTokenConfig;
import com.stablepay.testutil.AuthFixtures;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(AuthController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class, AuthResponseMapperImpl.class})
class AuthControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int EXPIRES_IN_SECONDS = 900;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocialLoginHandler socialLoginHandler;

    @MockitoBean
    private RefreshTokenHandler refreshTokenHandler;

    @MockitoBean
    private LogoutHandler logoutHandler;

    @MockitoSpyBean
    private AuthResponseMapper authResponseMapper;

    @MockitoBean
    private AuthTokenConfig authTokenConfig;

    @Nested
    class SocialLogin {

        @Test
        @SneakyThrows
        void shouldReturn201ForNewUser() {
            // given
            var wallet = walletBuilder()
                    .availableBalance(BigDecimal.ZERO)
                    .totalBalance(BigDecimal.ZERO)
                    .build();
            var loginResult = AuthFixtures.loginResultBuilder(wallet)
                    .newUser(true)
                    .build();

            given(socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN, "127.0.0.1", null)).willReturn(loginResult);
            given(authTokenConfig.accessTtl()).willReturn(Duration.ofMinutes(15));

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
                    .andExpect(jsonPath("$.accessToken").value(SOME_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(EXPIRES_IN_SECONDS))
                    .andExpect(jsonPath("$.user.id").value(SOME_AUTH_USER_ID.toString()))
                    .andExpect(jsonPath("$.user.email").value(SOME_EMAIL));
        }

        @Test
        @SneakyThrows
        void shouldReturn200ForReturningUser() {
            // given
            var wallet = walletBuilder()
                    .availableBalance(BigDecimal.ZERO)
                    .totalBalance(BigDecimal.ZERO)
                    .build();
            var loginResult = AuthFixtures.loginResultBuilder(wallet)
                    .newUser(false)
                    .build();

            given(socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN, "127.0.0.1", null)).willReturn(loginResult);
            given(authTokenConfig.accessTtl()).willReturn(Duration.ofMinutes(15));

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk());
        }

        @Test
        @SneakyThrows
        void shouldReturn401WhenIdTokenInvalid() {
            // given
            given(socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN, "127.0.0.1", null))
                    .willThrow(InvalidIdTokenException.of("bad signature"));

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("SP-0032"));
        }

        @Test
        @SneakyThrows
        void shouldReturn400WhenProviderUnsupported() {
            // given
            given(socialLoginHandler.handle("facebook", SOME_ID_TOKEN, "127.0.0.1", null))
                    .willThrow(UnsupportedAuthProviderException.forProvider("facebook"));

            var request = SocialLoginRequest.builder()
                    .provider("facebook")
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SP-0034"));
        }

        @Test
        @SneakyThrows
        void shouldReturn400WhenRequestInvalid() {
            // given
            var request = SocialLoginRequest.builder()
                    .provider("")
                    .idToken("")
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SP-0003"));
        }
    }

    @Nested
    class RefreshToken {

        @Test
        @SneakyThrows
        void shouldReturn200WithNewTokens() {
            // given
            var session = authSessionBuilder().build();

            given(refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN, "127.0.0.1", null)).willReturn(session);
            given(authTokenConfig.accessTtl()).willReturn(Duration.ofMinutes(15));

            var request = RefreshTokenRequest.builder()
                    .refreshToken(SOME_RAW_REFRESH_TOKEN)
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(SOME_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.refreshToken").value(SOME_RAW_REFRESH_TOKEN));
        }

        @Test
        @SneakyThrows
        void shouldReturn401WhenRefreshTokenInvalid() {
            // given
            given(refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN, "127.0.0.1", null))
                    .willThrow(InvalidRefreshTokenException.of("not found"));

            var request = RefreshTokenRequest.builder()
                    .refreshToken(SOME_RAW_REFRESH_TOKEN)
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("SP-0035"));
        }

        @Test
        @SneakyThrows
        void shouldReturn400WhenRefreshTokenBlank() {
            // given
            var request = RefreshTokenRequest.builder()
                    .refreshToken("")
                    .build();

            // when
            var result = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SP-0003"));
        }
    }

    @Nested
    class Logout {

        @Test
        @SneakyThrows
        void shouldReturn204OnSuccessfulLogout() {
            // given

            // when
            var result = mockMvc.perform(post("/api/auth/logout")
                            .with(asUser(SOME_AUTH_USER_ID)));

            // then
            result.andExpect(status().isNoContent());
            then(logoutHandler).should().handle(SOME_AUTH_USER_ID, "127.0.0.1", null);
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
