package com.stablepay.infrastructure.auth.google;

import static com.stablepay.testutil.AuthFixtures.SOME_ID_TOKEN;
import static com.stablepay.testutil.AuthFixtures.SOME_PROVIDER;
import static com.stablepay.testutil.AuthFixtures.SOME_SOCIAL_EMAIL;
import static com.stablepay.testutil.AuthFixtures.SOME_SUBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.stablepay.domain.auth.exception.EmailNotVerifiedException;
import com.stablepay.domain.auth.exception.InvalidIdTokenException;
import com.stablepay.domain.auth.exception.UnsupportedAuthProviderException;
import com.stablepay.domain.auth.model.SocialIdentity;

@ExtendWith(MockitoExtension.class)
class GoogleIdTokenVerifierAdapterTest {

    @Mock
    private JwtDecoder googleJwtDecoder;

    private GoogleIdTokenVerifierAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GoogleIdTokenVerifierAdapter(googleJwtDecoder);
    }

    @Test
    void shouldVerifyValidGoogleIdTokenAndReturnSocialIdentity() {
        // given
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", SOME_SUBJECT)
                .claim("email", SOME_SOCIAL_EMAIL)
                .claim("email_verified", true)
                .build();
        given(googleJwtDecoder.decode(SOME_ID_TOKEN)).willReturn(jwt);

        // when
        var result = adapter.verify(SOME_PROVIDER, SOME_ID_TOKEN);

        // then
        var expected = SocialIdentity.builder()
                .provider(SOME_PROVIDER)
                .subject(SOME_SUBJECT)
                .email(SOME_SOCIAL_EMAIL)
                .emailVerified(true)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowInvalidIdTokenExceptionWhenTokenIsInvalid() {
        // given
        given(googleJwtDecoder.decode(SOME_ID_TOKEN)).willThrow(new JwtException("invalid"));

        // when
        // then
        assertThatThrownBy(() -> adapter.verify(SOME_PROVIDER, SOME_ID_TOKEN))
                .isInstanceOf(InvalidIdTokenException.class);
    }

    @Test
    void shouldThrowEmailNotVerifiedExceptionWhenEmailNotVerified() {
        // given
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", SOME_SUBJECT)
                .claim("email", SOME_SOCIAL_EMAIL)
                .claim("email_verified", false)
                .build();
        given(googleJwtDecoder.decode(SOME_ID_TOKEN)).willReturn(jwt);

        // when
        // then
        assertThatThrownBy(() -> adapter.verify(SOME_PROVIDER, SOME_ID_TOKEN))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    void shouldThrowUnsupportedAuthProviderExceptionForNonGoogleProvider() {
        // given
        // when
        // then
        assertThatThrownBy(() -> adapter.verify("apple", SOME_ID_TOKEN))
                .isInstanceOf(UnsupportedAuthProviderException.class);
    }
}
