package com.stablepay.infrastructure.db.user;

import static com.stablepay.testutil.AuthFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.AuthFixtures.SOME_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.auth.model.AppUser;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private UserJpaRepository jpaRepository;

    @Spy
    private UserMapper mapper = new UserMapperImpl();

    @InjectMocks
    private UserRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<UserEntity> entityCaptor;

    @Test
    void shouldFindUserByIdAndMapToDomain() {
        // given
        var entity = UserEntity.builder()
                .id(SOME_USER_ID)
                .email(SOME_EMAIL)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_CREATED_AT)
                .build();
        given(jpaRepository.findById(SOME_USER_ID)).willReturn(Optional.of(entity));

        // when
        var result = adapter.findById(SOME_USER_ID);

        // then
        var expected = AppUser.builder()
                .id(SOME_USER_ID)
                .email(SOME_EMAIL)
                .createdAt(SOME_CREATED_AT)
                .build();
        assertThat(result).isPresent();
        assertThat(result.get())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        // given
        given(jpaRepository.findById(SOME_USER_ID)).willReturn(Optional.empty());

        // when
        var result = adapter.findById(SOME_USER_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveUserWithTimestamps() {
        // given
        var user = AppUser.builder()
                .id(SOME_USER_ID)
                .email(SOME_EMAIL)
                .build();
        var savedEntity = UserEntity.builder()
                .id(SOME_USER_ID)
                .email(SOME_EMAIL)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_CREATED_AT)
                .build();
        given(jpaRepository.save(entityCaptor.capture())).willReturn(savedEntity);

        // when
        var result = adapter.save(user);

        // then
        var captured = entityCaptor.getValue();
        assertThat(captured.getCreatedAt()).isNotNull();
        assertThat(captured.getUpdatedAt()).isNotNull();

        var expected = AppUser.builder()
                .id(SOME_USER_ID)
                .email(SOME_EMAIL)
                .createdAt(SOME_CREATED_AT)
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldPreserveExistingCreatedAtOnSave() {
        // given
        var user = AppUser.builder()
                .id(SOME_USER_ID)
                .email(SOME_EMAIL)
                .createdAt(SOME_CREATED_AT)
                .build();
        var savedEntity = UserEntity.builder()
                .id(SOME_USER_ID)
                .email(SOME_EMAIL)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_CREATED_AT)
                .build();
        given(jpaRepository.save(entityCaptor.capture())).willReturn(savedEntity);

        // when
        adapter.save(user);

        // then
        var captured = entityCaptor.getValue();
        assertThat(captured.getCreatedAt()).isEqualTo(SOME_CREATED_AT);
        then(jpaRepository).should().save(captured);
    }
}
