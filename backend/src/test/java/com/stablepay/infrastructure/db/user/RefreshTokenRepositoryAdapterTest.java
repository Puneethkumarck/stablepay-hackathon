package com.stablepay.infrastructure.db.user;

import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_REFRESH_EXPIRES_AT;
import static com.stablepay.testutil.AuthFixtures.SOME_REFRESH_TOKEN_ID;
import static com.stablepay.testutil.AuthFixtures.SOME_TOKEN_HASH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.auth.model.RefreshToken;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryAdapterTest {

    @Mock
    private RefreshTokenJpaRepository jpaRepository;

    @Spy
    private UserMapper mapper = new UserMapperImpl();

    @InjectMocks
    private RefreshTokenRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<RefreshTokenEntity> entityCaptor;

    @Test
    void shouldFindByHashAndMapToDomain() {
        // given
        var entity = RefreshTokenEntity.builder()
                .id(SOME_REFRESH_TOKEN_ID)
                .userId(SOME_AUTH_USER_ID)
                .tokenHash(SOME_TOKEN_HASH)
                .issuedAt(Instant.now())
                .expiresAt(SOME_REFRESH_EXPIRES_AT)
                .revokedAt(null)
                .build();
        given(jpaRepository.findByTokenHash(SOME_TOKEN_HASH)).willReturn(Optional.of(entity));

        // when
        var result = adapter.findByHash(SOME_TOKEN_HASH);

        // then
        var expected = RefreshToken.builder()
                .id(SOME_REFRESH_TOKEN_ID)
                .userId(SOME_AUTH_USER_ID)
                .tokenHash(SOME_TOKEN_HASH)
                .expiresAt(SOME_REFRESH_EXPIRES_AT)
                .revokedAt(null)
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(Optional.of(expected));
    }

    @Test
    void shouldReturnEmptyWhenHashNotFound() {
        // given
        given(jpaRepository.findByTokenHash(SOME_TOKEN_HASH)).willReturn(Optional.empty());

        // when
        var result = adapter.findByHash(SOME_TOKEN_HASH);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveWithIssuedAtTimestamp() {
        // given
        var token = RefreshToken.builder()
                .id(SOME_REFRESH_TOKEN_ID)
                .userId(SOME_AUTH_USER_ID)
                .tokenHash(SOME_TOKEN_HASH)
                .expiresAt(SOME_REFRESH_EXPIRES_AT)
                .build();
        var savedEntity = RefreshTokenEntity.builder()
                .id(SOME_REFRESH_TOKEN_ID)
                .userId(SOME_AUTH_USER_ID)
                .tokenHash(SOME_TOKEN_HASH)
                .issuedAt(Instant.now())
                .expiresAt(SOME_REFRESH_EXPIRES_AT)
                .build();
        given(jpaRepository.save(entityCaptor.capture())).willReturn(savedEntity);

        // when
        var result = adapter.save(token);

        // then
        var captured = entityCaptor.getValue();
        var expectedEntity = RefreshTokenEntity.builder()
                .id(SOME_REFRESH_TOKEN_ID)
                .userId(SOME_AUTH_USER_ID)
                .tokenHash(SOME_TOKEN_HASH)
                .expiresAt(SOME_REFRESH_EXPIRES_AT)
                .build();
        assertThat(captured)
                .usingRecursiveComparison()
                .ignoringFields("issuedAt")
                .isEqualTo(expectedEntity);
        assertThat(captured.getIssuedAt()).isNotNull();

        var expected = RefreshToken.builder()
                .id(SOME_REFRESH_TOKEN_ID)
                .userId(SOME_AUTH_USER_ID)
                .tokenHash(SOME_TOKEN_HASH)
                .expiresAt(SOME_REFRESH_EXPIRES_AT)
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldDelegateRevokeByUserId() {
        // given

        // when
        adapter.revokeByUserId(SOME_AUTH_USER_ID);

        // then
        then(jpaRepository).should().revokeByUserId(SOME_AUTH_USER_ID);
    }
}
