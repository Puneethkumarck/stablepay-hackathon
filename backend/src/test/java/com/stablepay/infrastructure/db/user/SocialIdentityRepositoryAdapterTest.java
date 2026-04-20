package com.stablepay.infrastructure.db.user;

import static com.stablepay.testutil.AuthFixtures.SOME_PROVIDER;
import static com.stablepay.testutil.AuthFixtures.SOME_SOCIAL_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_SUBJECT;
import static com.stablepay.testutil.AuthFixtures.SOME_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.auth.model.SocialIdentity;

@ExtendWith(MockitoExtension.class)
class SocialIdentityRepositoryAdapterTest {

    @Mock
    private SocialIdentityJpaRepository jpaRepository;

    @Spy
    private UserMapper mapper = new UserMapperImpl();

    @InjectMocks
    private SocialIdentityRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<SocialIdentityEntity> entityCaptor;

    @Test
    void shouldFindByProviderAndSubjectAndMapToDomain() {
        // given
        var entity = SocialIdentityEntity.builder()
                .id(java.util.UUID.randomUUID())
                .userId(SOME_USER_ID)
                .provider(SOME_PROVIDER)
                .subject(SOME_SUBJECT)
                .email(SOME_SOCIAL_EMAIL)
                .emailVerified(true)
                .createdAt(java.time.Instant.now())
                .build();
        given(jpaRepository.findByProviderAndSubject(SOME_PROVIDER, SOME_SUBJECT))
                .willReturn(Optional.of(entity));

        // when
        var result = adapter.findByProviderAndSubject(SOME_PROVIDER, SOME_SUBJECT);

        // then
        var expected = SocialIdentity.builder()
                .provider(SOME_PROVIDER)
                .subject(SOME_SUBJECT)
                .email(SOME_SOCIAL_EMAIL)
                .emailVerified(true)
                .build();
        assertThat(result).isPresent();
        assertThat(result.get())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        // given
        given(jpaRepository.findByProviderAndSubject(SOME_PROVIDER, SOME_SUBJECT))
                .willReturn(Optional.empty());

        // when
        var result = adapter.findByProviderAndSubject(SOME_PROVIDER, SOME_SUBJECT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveWithGeneratedIdAndUserId() {
        // given
        var identity = SocialIdentity.builder()
                .provider(SOME_PROVIDER)
                .subject(SOME_SUBJECT)
                .email(SOME_SOCIAL_EMAIL)
                .emailVerified(true)
                .build();
        var savedEntity = SocialIdentityEntity.builder()
                .id(java.util.UUID.randomUUID())
                .userId(SOME_USER_ID)
                .provider(SOME_PROVIDER)
                .subject(SOME_SUBJECT)
                .email(SOME_SOCIAL_EMAIL)
                .emailVerified(true)
                .createdAt(java.time.Instant.now())
                .build();
        given(jpaRepository.save(entityCaptor.capture())).willReturn(savedEntity);

        // when
        var result = adapter.save(identity, SOME_USER_ID);

        // then
        var captured = entityCaptor.getValue();
        assertThat(captured.getId()).isNotNull();
        assertThat(captured.getUserId()).isEqualTo(SOME_USER_ID);
        assertThat(captured.getCreatedAt()).isNotNull();

        var expected = SocialIdentity.builder()
                .provider(SOME_PROVIDER)
                .subject(SOME_SUBJECT)
                .email(SOME_SOCIAL_EMAIL)
                .emailVerified(true)
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
