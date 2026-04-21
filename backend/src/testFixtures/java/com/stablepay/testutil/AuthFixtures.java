package com.stablepay.testutil;

import java.time.Instant;
import java.util.UUID;

import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.model.RefreshToken;
import com.stablepay.domain.auth.model.SocialIdentity;
import com.stablepay.domain.auth.port.UserRepository;

public final class AuthFixtures {

    private AuthFixtures() {}

    public static final UUID SOME_AUTH_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
    public static final String SOME_EMAIL = "alice@example.com";
    public static final Instant SOME_AUTH_CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");

    public static final String SOME_PROVIDER = "google";
    public static final String SOME_SUBJECT = "google-sub-12345";
    public static final String SOME_SOCIAL_EMAIL = "alice@gmail.com";

    public static final UUID SOME_REFRESH_TOKEN_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final String SOME_TOKEN_HASH = "sha256-abc123def456";
    public static final Instant SOME_REFRESH_EXPIRES_AT = Instant.parse("2026-05-03T10:00:00Z");

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

    public static UUID createTestUser(UserRepository userRepository) {
        var userId = UUID.randomUUID();
        userRepository.save(AppUser.builder().id(userId).email(userId + "@test.com").build());
        return userId;
    }
}
