package com.stablepay.infrastructure.db.user;

import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_CREATED_AT;
import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_PROVIDER;
import static com.stablepay.testutil.AuthFixtures.SOME_REFRESH_EXPIRES_AT;
import static com.stablepay.testutil.AuthFixtures.SOME_REFRESH_TOKEN_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_SOCIAL_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_SUBJECT;
import static com.stablepay.testutil.AuthFixtures.SOME_TOKEN_HASH;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.model.RefreshToken;
import com.stablepay.domain.auth.model.SocialIdentity;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Nested
    class AppUserMapping {

        @Test
        void shouldMapDomainToEntity() {
            // given
            var domain = AppUser.builder()
                    .id(SOME_AUTH_USER_ID)
                    .email(SOME_EMAIL)
                    .createdAt(SOME_AUTH_CREATED_AT)
                    .build();

            // when
            var entity = mapper.toEntity(domain);

            // then
            var expectedEntity = UserEntity.builder()
                    .id(SOME_AUTH_USER_ID)
                    .email(SOME_EMAIL)
                    .createdAt(SOME_AUTH_CREATED_AT)
                    .build();
            assertThat(entity)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedEntity);
        }

        @Test
        void shouldMapEntityToDomainWithAllFields() {
            // given
            var entity = UserEntity.builder()
                    .id(SOME_AUTH_USER_ID)
                    .email(SOME_EMAIL)
                    .createdAt(SOME_AUTH_CREATED_AT)
                    .updatedAt(Instant.parse("2026-04-03T12:00:00Z"))
                    .build();

            // when
            var domain = mapper.toDomain(entity);

            // then
            var expected = AppUser.builder()
                    .id(SOME_AUTH_USER_ID)
                    .email(SOME_EMAIL)
                    .createdAt(SOME_AUTH_CREATED_AT)
                    .build();

            assertThat(domain)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }

    @Nested
    class SocialIdentityMapping {

        @Test
        void shouldMapEntityToDomainIncludingUserId() {
            // given
            var entity = SocialIdentityEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(SOME_AUTH_USER_ID)
                    .provider(SOME_PROVIDER)
                    .subject(SOME_SUBJECT)
                    .email(SOME_SOCIAL_EMAIL)
                    .emailVerified(true)
                    .createdAt(SOME_AUTH_CREATED_AT)
                    .build();

            // when
            var domain = mapper.toDomain(entity);

            // then
            var expected = SocialIdentity.builder()
                    .userId(SOME_AUTH_USER_ID)
                    .provider(SOME_PROVIDER)
                    .subject(SOME_SUBJECT)
                    .email(SOME_SOCIAL_EMAIL)
                    .emailVerified(true)
                    .build();

            assertThat(domain)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldMapDomainToEntityIgnoringInfraFields() {
            // given
            var domain = SocialIdentity.builder()
                    .userId(SOME_AUTH_USER_ID)
                    .provider(SOME_PROVIDER)
                    .subject(SOME_SUBJECT)
                    .email(SOME_SOCIAL_EMAIL)
                    .emailVerified(true)
                    .build();

            // when
            var entity = mapper.toEntity(domain);

            // then
            var expectedEntity = SocialIdentityEntity.builder()
                    .userId(SOME_AUTH_USER_ID)
                    .provider(SOME_PROVIDER)
                    .subject(SOME_SUBJECT)
                    .email(SOME_SOCIAL_EMAIL)
                    .emailVerified(true)
                    .build();
            assertThat(entity)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedEntity);
        }
    }

    @Nested
    class RefreshTokenMapping {

        @Test
        void shouldMapEntityToDomainDroppingIssuedAt() {
            // given
            var entity = RefreshTokenEntity.builder()
                    .id(SOME_REFRESH_TOKEN_ID)
                    .userId(SOME_AUTH_USER_ID)
                    .tokenHash(SOME_TOKEN_HASH)
                    .issuedAt(Instant.parse("2026-04-03T10:00:00Z"))
                    .expiresAt(SOME_REFRESH_EXPIRES_AT)
                    .revokedAt(null)
                    .build();

            // when
            var domain = mapper.toDomain(entity);

            // then
            var expected = RefreshToken.builder()
                    .id(SOME_REFRESH_TOKEN_ID)
                    .userId(SOME_AUTH_USER_ID)
                    .tokenHash(SOME_TOKEN_HASH)
                    .expiresAt(SOME_REFRESH_EXPIRES_AT)
                    .revokedAt(null)
                    .build();

            assertThat(domain)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldMapDomainToEntityIgnoringIssuedAt() {
            // given
            var domain = RefreshToken.builder()
                    .id(SOME_REFRESH_TOKEN_ID)
                    .userId(SOME_AUTH_USER_ID)
                    .tokenHash(SOME_TOKEN_HASH)
                    .expiresAt(SOME_REFRESH_EXPIRES_AT)
                    .build();

            // when
            var entity = mapper.toEntity(domain);

            // then
            var expectedEntity = RefreshTokenEntity.builder()
                    .id(SOME_REFRESH_TOKEN_ID)
                    .userId(SOME_AUTH_USER_ID)
                    .tokenHash(SOME_TOKEN_HASH)
                    .expiresAt(SOME_REFRESH_EXPIRES_AT)
                    .build();
            assertThat(entity)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedEntity);
        }
    }
}
