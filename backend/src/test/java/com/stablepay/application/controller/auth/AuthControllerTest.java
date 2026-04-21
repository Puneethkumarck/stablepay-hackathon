package com.stablepay.application.controller.auth;

import static com.stablepay.testutil.AuthFixtures.SOME_ACCESS_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_ID_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_PROVIDER;
import static com.stablepay.testutil.AuthFixtures.SOME_RAW_REFRESH_TOKEN;
import static com.stablepay.testutil.AuthFixtures.authSessionBuilder;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.config.SecurityConfig;
import com.stablepay.application.controller.auth.mapper.AuthResponseMapper;
import com.stablepay.application.dto.AuthResponse;
import com.stablepay.application.dto.RefreshTokenRequest;
import com.stablepay.application.dto.SocialLoginRequest;
import com.stablepay.application.dto.UserResponse;
import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.auth.exception.InvalidIdTokenException;
import com.stablepay.domain.auth.exception.InvalidRefreshTokenException;
import com.stablepay.domain.auth.exception.UnsupportedAuthProviderException;
import com.stablepay.domain.auth.handler.LogoutHandler;
import com.stablepay.domain.auth.handler.RefreshTokenHandler;
import com.stablepay.domain.auth.handler.SocialLoginHandler;
import com.stablepay.domain.auth.model.AuthPrincipal;
import com.stablepay.domain.auth.model.AuthTokenConfig;
import com.stablepay.testutil.AuthFixtures;
import com.stablepay.testutil.TestClockConfig;

import lombok.SneakyThrows;

@WebMvcTest(AuthController.class)
@Import({TestClockConfig.class, SecurityConfig.class})
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

    @MockitoBean
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
            var authResponse = AuthResponse.builder()
                    .accessToken(SOME_ACCESS_TOKEN)
                    .refreshToken(SOME_RAW_REFRESH_TOKEN)
                    .tokenType("Bearer")
                    .expiresIn(EXPIRES_IN_SECONDS)
                    .user(UserResponse.builder().id(SOME_AUTH_USER_ID).email(SOME_EMAIL).build())
                    .wallet(WalletResponse.builder().build())
                    .build();

            given(socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN)).willReturn(loginResult);
            given(authTokenConfig.accessTtl()).willReturn(Duration.ofMinutes(15));
            given(authResponseMapper.toResponse(loginResult, EXPIRES_IN_SECONDS)).willReturn(authResponse);

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when / then
            mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value(SOME_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(EXPIRES_IN_SECONDS));
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
            var authResponse = AuthResponse.builder()
                    .accessToken(SOME_ACCESS_TOKEN)
                    .refreshToken(SOME_RAW_REFRESH_TOKEN)
                    .tokenType("Bearer")
                    .expiresIn(EXPIRES_IN_SECONDS)
                    .build();

            given(socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN)).willReturn(loginResult);
            given(authTokenConfig.accessTtl()).willReturn(Duration.ofMinutes(15));
            given(authResponseMapper.toResponse(loginResult, EXPIRES_IN_SECONDS)).willReturn(authResponse);

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when / then
            mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @SneakyThrows
        void shouldReturn401WhenIdTokenInvalid() {
            // given
            given(socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN))
                    .willThrow(InvalidIdTokenException.of("bad signature"));

            var request = SocialLoginRequest.builder()
                    .provider(SOME_PROVIDER)
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when / then
            mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("SP-0032"));
        }

        @Test
        @SneakyThrows
        void shouldReturn400WhenProviderUnsupported() {
            // given
            given(socialLoginHandler.handle("facebook", SOME_ID_TOKEN))
                    .willThrow(UnsupportedAuthProviderException.forProvider("facebook"));

            var request = SocialLoginRequest.builder()
                    .provider("facebook")
                    .idToken(SOME_ID_TOKEN)
                    .build();

            // when / then
            mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
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

            // when / then
            mockMvc.perform(post("/api/auth/social")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
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
            var authResponse = AuthResponse.builder()
                    .accessToken(SOME_ACCESS_TOKEN)
                    .refreshToken(SOME_RAW_REFRESH_TOKEN)
                    .tokenType("Bearer")
                    .expiresIn(EXPIRES_IN_SECONDS)
                    .build();

            given(refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN)).willReturn(session);
            given(authTokenConfig.accessTtl()).willReturn(Duration.ofMinutes(15));
            given(authResponseMapper.toRefreshResponse(session, EXPIRES_IN_SECONDS)).willReturn(authResponse);

            var request = RefreshTokenRequest.builder()
                    .refreshToken(SOME_RAW_REFRESH_TOKEN)
                    .build();

            // when / then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(SOME_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @SneakyThrows
        void shouldReturn401WhenRefreshTokenInvalid() {
            // given
            given(refreshTokenHandler.handle(SOME_RAW_REFRESH_TOKEN))
                    .willThrow(InvalidRefreshTokenException.of("not found"));

            var request = RefreshTokenRequest.builder()
                    .refreshToken(SOME_RAW_REFRESH_TOKEN)
                    .build();

            // when / then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("SP-0035"));
        }

        @Test
        @SneakyThrows
        void shouldReturn400WhenRefreshTokenBlank() {
            // given
            var request = RefreshTokenRequest.builder()
                    .refreshToken("")
                    .build();

            // when / then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(OBJECT_MAPPER.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SP-0003"));
        }
    }

    @Nested
    class Logout {

        @Test
        @SneakyThrows
        void shouldReturn204OnSuccessfulLogout() {
            // given
            var principal = AuthPrincipal.builder().id(SOME_AUTH_USER_ID).build();
            var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                    .header("alg", "HS256")
                    .subject(SOME_AUTH_USER_ID.toString())
                    .build();
            var authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                    jwt, java.util.Collections.emptyList(), SOME_AUTH_USER_ID.toString()) {
                @Override
                public Object getPrincipal() {
                    return principal;
                }
            };

            // when / then
            mockMvc.perform(post("/api/auth/logout")
                            .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                    .andExpect(status().isNoContent());

            then(logoutHandler).should().handle(SOME_AUTH_USER_ID);
        }
    }
}
