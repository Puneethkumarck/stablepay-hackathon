package com.stablepay.testutil;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.model.AuthPrincipal;
import com.stablepay.domain.auth.model.AuthSession;
import com.stablepay.domain.auth.model.LoginResult;
import com.stablepay.domain.auth.model.RefreshToken;
import com.stablepay.domain.auth.model.SocialIdentity;
import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.wallet.model.Wallet;

public final class AuthFixtures {

    private AuthFixtures() {}

    public static final String SOME_JWT_SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long!!";
    public static final Duration SOME_ACCESS_TTL = Duration.ofMinutes(15);
    public static final Duration SOME_REFRESH_TTL = Duration.ofDays(30);

    public static final UUID SOME_AUTH_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
    public static final String SOME_EMAIL = "alice@example.com";
    public static final Instant SOME_AUTH_CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");

    public static final String SOME_PROVIDER = "google";
    public static final String SOME_SUBJECT = "google-sub-12345";
    public static final String SOME_SOCIAL_EMAIL = "alice@gmail.com";
    public static final String SOME_ID_TOKEN = "some-google-id-token";
    public static final String SOME_GOOGLE_CLIENT_ID = "test-google-client-id.apps.googleusercontent.com";

    public static final UUID SOME_REFRESH_TOKEN_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final String SOME_TOKEN_HASH = "sha256-abc123def456";
    public static final Instant SOME_REFRESH_EXPIRES_AT = Instant.parse("2026-05-03T10:00:00Z");

    public static final String SOME_RAW_REFRESH_TOKEN = "r1_dGVzdC1yZWZyZXNoLXRva2Vu";
    public static final String SOME_ACCESS_TOKEN = "test-access-token";
    public static final Instant SOME_ACCESS_EXPIRES_AT = Instant.parse("2026-04-03T10:15:00Z");
    public static final String SOME_SENDER_DISPLAY_NAME = "alice";
    public static final UUID SOME_OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000077");

    public static AppUser.AppUserBuilder appUserBuilder() {
        return AppUser.builder()
                .id(SOME_AUTH_USER_ID)
                .email(SOME_EMAIL)
                .createdAt(SOME_AUTH_CREATED_AT);
    }

    public static SocialIdentity.SocialIdentityBuilder socialIdentityBuilder() {
        return SocialIdentity.builder()
                .userId(SOME_AUTH_USER_ID)
                .provider(SOME_PROVIDER)
                .subject(SOME_SUBJECT)
                .email(SOME_SOCIAL_EMAIL)
                .emailVerified(true);
    }

    public static RefreshToken.RefreshTokenBuilder refreshTokenBuilder() {
        return RefreshToken.builder()
                .id(SOME_REFRESH_TOKEN_ID)
                .userId(SOME_AUTH_USER_ID)
                .tokenHash(SOME_TOKEN_HASH)
                .expiresAt(SOME_REFRESH_EXPIRES_AT);
    }

    public static AuthSession.AuthSessionBuilder authSessionBuilder() {
        return AuthSession.builder()
                .accessToken(SOME_ACCESS_TOKEN)
                .refreshToken(SOME_RAW_REFRESH_TOKEN)
                .accessExpiresAt(SOME_ACCESS_EXPIRES_AT);
    }

    public static LoginResult.LoginResultBuilder loginResultBuilder(Wallet wallet) {
        return LoginResult.builder()
                .session(authSessionBuilder().build())
                .user(appUserBuilder().build())
                .wallet(wallet)
                .newUser(true);
    }

    public static UUID createTestUser(UserRepository userRepository) {
        var userId = UUID.randomUUID();
        userRepository.save(AppUser.builder().id(userId).email(userId + "@test.com").build());
        return userId;
    }

    public static Authentication authenticationFor(UUID userId) {
        var jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
        var principal = AuthPrincipal.builder().id(userId).build();
        return new JwtAuthenticationToken(jwt, Collections.emptyList(), principal.id().toString()) {
            @Override
            public Object getPrincipal() {
                return principal;
            }
        };
    }
}
