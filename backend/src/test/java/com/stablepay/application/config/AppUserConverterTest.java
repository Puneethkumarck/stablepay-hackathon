package com.stablepay.application.config;

import static com.stablepay.testutil.AuthFixtures.SOME_AUTH_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.stablepay.domain.auth.model.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class AppUserConverterTest {

    @Mock
    private Jwt jwt;

    @InjectMocks
    private AppUserConverter converter;

    @Test
    void shouldConvertJwtToAuthenticationTokenWithAuthPrincipal() {
        // given
        given(jwt.getSubject()).willReturn(SOME_AUTH_USER_ID.toString());

        // when
        var result = converter.convert(jwt);

        // then
        var expected = AuthPrincipal.builder().id(SOME_AUTH_USER_ID).build();
        assertThat(result.getPrincipal())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
