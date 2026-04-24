package com.stablepay.domain.auth.handler;

import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_CREATED_AT;
import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_ID_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_PROVIDER;
import static com.stablepay.testutil.AuthFixtures.SOME_RAW_REFRESH_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_SOCIAL_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_SUBJECT;
import static com.stablepay.testutil.AuthFixtures.SOME_TOKEN_HASH;
import static com.stablepay.testutil.AuthFixtures.SOME_USER_NAME;
import static com.stablepay.testutil.AuthFixtures.appUserBuilder;
import static com.stablepay.testutil.AuthFixtures.authSessionBuilder;
import static com.stablepay.testutil.AuthFixtures.socialIdentityBuilder;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.auth.exception.EmailNotVerifiedException;
import com.stablepay.domain.auth.model.AuthTokenConfig;
import com.stablepay.domain.auth.model.LoginResult;
import com.stablepay.domain.auth.port.AuthTokenIssuer;
import com.stablepay.domain.auth.port.RefreshTokenRepository;
import com.stablepay.domain.auth.port.SocialIdentityRepository;
import com.stablepay.domain.auth.port.SocialIdentityVerifier;
import com.stablepay.domain.auth.port.TokenHasher;
import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.wallet.handler.CreateWalletHandler;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class SocialLoginHandlerTest {

    @Mock
    private SocialIdentityVerifier socialIdentityVerifier;

    @Mock
    private SocialIdentityRepository socialIdentityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreateWalletHandler createWalletHandler;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final AuthTokenConfig authTokenConfig = AuthTokenConfig.builder()
            .accessTtl(Duration.ofMinutes(15))
            .refreshTtl(Duration.ofDays(30))
            .build();

    private final Clock clock = Clock.fixed(SOME_AUTH_CREATED_AT, ZoneOffset.UTC);

    private SocialLoginHandler socialLoginHandler;

    @BeforeEach
    void setUp() {
        socialLoginHandler = new SocialLoginHandler(
                socialIdentityVerifier,
                socialIdentityRepository,
                userRepository,
                createWalletHandler,
                walletRepository,
                authTokenIssuer,
                tokenHasher,
                refreshTokenRepository,
                authTokenConfig,
                clock);
    }

    @Test
    void shouldCreateNewUserWithWalletOnFirstLogin() {
        // given
        var identity = socialIdentityBuilder().userId(null).build();
        given(socialIdentityVerifier.verify(SOME_PROVIDER, SOME_ID_TOKEN)).willReturn(identity);
        given(socialIdentityRepository.findByProviderAndSubject(SOME_PROVIDER, SOME_SUBJECT))
                .willReturn(Optional.empty());

        var appUser = appUserBuilder().build();
        given(userRepository.save(argThat(user ->
                user.email().equals(SOME_SOCIAL_EMAIL)
                        && user.name().equals(SOME_USER_NAME)
                        && user.createdAt().equals(SOME_AUTH_CREATED_AT))))
                .willReturn(appUser);

        var savedIdentity = identity.toBuilder().userId(SOME_AUTH_USER_ID).build();
        given(socialIdentityRepository.save(savedIdentity)).willReturn(savedIdentity);

        var wallet = walletBuilder().userId(SOME_AUTH_USER_ID).build();
        given(createWalletHandler.handle(SOME_AUTH_USER_ID)).willReturn(wallet);

        var session = authSessionBuilder().build();
        given(authTokenIssuer.issue(SOME_AUTH_USER_ID)).willReturn(session);
        given(tokenHasher.hash(SOME_RAW_REFRESH_TOKEN)).willReturn(SOME_TOKEN_HASH);

        // when
        var result = socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN, "127.0.0.1", "TestAgent");

        // then
        var expected = LoginResult.builder()
                .session(session)
                .user(appUser)
                .wallet(wallet)
                .newUser(true)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
        then(refreshTokenRepository).should().revokeByUserId(SOME_AUTH_USER_ID);
        then(refreshTokenRepository).should().save(argThat(rt ->
                rt.userId().equals(SOME_AUTH_USER_ID)
                        && rt.tokenHash().equals(SOME_TOKEN_HASH)
                        && rt.expiresAt().equals(SOME_AUTH_CREATED_AT.plus(Duration.ofDays(30)))));
    }

    @Test
    void shouldReturnExistingUserOnReturningLogin() {
        // given
        var identity = socialIdentityBuilder().userId(null).build();
        given(socialIdentityVerifier.verify(SOME_PROVIDER, SOME_ID_TOKEN)).willReturn(identity);

        var existingIdentity = socialIdentityBuilder().build();
        given(socialIdentityRepository.findByProviderAndSubject(SOME_PROVIDER, SOME_SUBJECT))
                .willReturn(Optional.of(existingIdentity));

        var appUser = appUserBuilder().build();
        given(userRepository.findById(SOME_AUTH_USER_ID)).willReturn(Optional.of(appUser));

        var wallet = walletBuilder().userId(SOME_AUTH_USER_ID).build();
        given(walletRepository.findByUserId(SOME_AUTH_USER_ID)).willReturn(Optional.of(wallet));

        var session = authSessionBuilder().build();
        given(authTokenIssuer.issue(SOME_AUTH_USER_ID)).willReturn(session);
        given(tokenHasher.hash(SOME_RAW_REFRESH_TOKEN)).willReturn(SOME_TOKEN_HASH);

        // when
        var result = socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN, "127.0.0.1", "TestAgent");

        // then
        var expected = LoginResult.builder()
                .session(session)
                .user(appUser)
                .wallet(wallet)
                .newUser(false)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
        then(refreshTokenRepository).should().revokeByUserId(SOME_AUTH_USER_ID);
        then(refreshTokenRepository).should().save(argThat(rt ->
                rt.userId().equals(SOME_AUTH_USER_ID)
                        && rt.tokenHash().equals(SOME_TOKEN_HASH)
                        && rt.expiresAt().equals(SOME_AUTH_CREATED_AT.plus(Duration.ofDays(30)))));
    }

    @Test
    void shouldPropagateEmailNotVerifiedException() {
        // given
        given(socialIdentityVerifier.verify(SOME_PROVIDER, SOME_ID_TOKEN))
                .willThrow(EmailNotVerifiedException.forEmail(SOME_SOCIAL_EMAIL));

        // when
        var thrown = catchThrowable(() -> socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN, "127.0.0.1", "TestAgent"));

        // then
        assertThat(thrown)
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    void shouldPropagateExceptionOnMpcFailure() {
        // given
        var identity = socialIdentityBuilder().userId(null).build();
        given(socialIdentityVerifier.verify(SOME_PROVIDER, SOME_ID_TOKEN)).willReturn(identity);
        given(socialIdentityRepository.findByProviderAndSubject(SOME_PROVIDER, SOME_SUBJECT))
                .willReturn(Optional.empty());

        var appUser = appUserBuilder().build();
        given(userRepository.save(argThat(user ->
                user.email().equals(SOME_SOCIAL_EMAIL)
                        && user.name().equals(SOME_USER_NAME)
                        && user.createdAt().equals(SOME_AUTH_CREATED_AT))))
                .willReturn(appUser);

        var savedIdentity = identity.toBuilder().userId(SOME_AUTH_USER_ID).build();
        given(socialIdentityRepository.save(savedIdentity)).willReturn(savedIdentity);

        given(createWalletHandler.handle(SOME_AUTH_USER_ID))
                .willThrow(new RuntimeException("MPC sidecar unavailable"));

        // when
        var thrown = catchThrowable(() -> socialLoginHandler.handle(SOME_PROVIDER, SOME_ID_TOKEN, "127.0.0.1", "TestAgent"));

        // then
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MPC sidecar unavailable");
    }
}
