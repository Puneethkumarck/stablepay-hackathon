package com.stablepay.application.controller.auth.mapper;

import static com.stablepay.testutil.AuthFixtures.SOME_ACCESS_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_CREATED_AT;
import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_RAW_REFRESH_TOKEN;
import static com.stablepay.testutil.AuthFixtures.authSessionBuilder;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.stablepay.application.dto.AuthResponse;
import com.stablepay.application.dto.UserResponse;
import com.stablepay.application.dto.WalletResponse;
import com.stablepay.testutil.AuthFixtures;

class AuthResponseMapperTest {

    private final AuthResponseMapper mapper = Mappers.getMapper(AuthResponseMapper.class);

    @Test
    void shouldMapLoginResultToAuthResponse() {
        // given
        var wallet = walletBuilder()
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();
        var loginResult = AuthFixtures.loginResultBuilder(wallet).build();

        // when
        var result = mapper.toResponse(loginResult, 900);

        // then
        var expected = AuthResponse.builder()
                .accessToken(SOME_ACCESS_TOKEN)
                .refreshToken(SOME_RAW_REFRESH_TOKEN)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(UserResponse.builder()
                        .id(SOME_AUTH_USER_ID)
                        .email(SOME_EMAIL)
                        .createdAt(SOME_AUTH_CREATED_AT)
                        .build())
                .wallet(WalletResponse.builder()
                        .id(wallet.id())
                        .solanaAddress(SOME_SOLANA_ADDRESS)
                        .availableBalance(BigDecimal.ZERO)
                        .totalBalance(BigDecimal.ZERO)
                        .createdAt(wallet.createdAt())
                        .updatedAt(wallet.updatedAt())
                        .build())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapAuthSessionToRefreshResponse() {
        // given
        var session = authSessionBuilder().build();

        // when
        var result = mapper.toRefreshResponse(session, 900);

        // then
        var expected = AuthResponse.builder()
                .accessToken(SOME_ACCESS_TOKEN)
                .refreshToken(SOME_RAW_REFRESH_TOKEN)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(null)
                .wallet(null)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
