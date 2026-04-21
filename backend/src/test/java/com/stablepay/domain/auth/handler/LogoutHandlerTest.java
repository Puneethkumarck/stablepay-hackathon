package com.stablepay.domain.auth.handler;

import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.auth.port.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class LogoutHandlerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private LogoutHandler logoutHandler;

    @Test
    void shouldRevokeAllRefreshTokensForUser() {
        // given

        // when
        logoutHandler.handle(SOME_AUTH_USER_ID, "127.0.0.1", "TestAgent");

        // then
        then(refreshTokenRepository).should().revokeByUserId(SOME_AUTH_USER_ID);
    }
}
