package com.stablepay.infrastructure.auth.google;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.stablepay.domain.auth.exception.EmailNotVerifiedException;
import com.stablepay.domain.auth.exception.InvalidIdTokenException;
import com.stablepay.domain.auth.exception.UnsupportedAuthProviderException;
import com.stablepay.domain.auth.model.SocialIdentity;
import com.stablepay.domain.auth.port.SocialIdentityVerifier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GoogleIdTokenVerifierAdapter implements SocialIdentityVerifier {

    private static final String GOOGLE_PROVIDER = "google";

    private final JwtDecoder googleJwtDecoder;

    public GoogleIdTokenVerifierAdapter(@Qualifier("googleJwtDecoder") JwtDecoder googleJwtDecoder) {
        this.googleJwtDecoder = googleJwtDecoder;
    }

    @Override
    public SocialIdentity verify(String provider, String idToken) {
        if (!GOOGLE_PROVIDER.equals(provider)) {
            throw UnsupportedAuthProviderException.forProvider(provider);
        }

        try {
            var jwt = googleJwtDecoder.decode(idToken);
            var sub = jwt.getClaimAsString("sub");
            var email = jwt.getClaimAsString("email");

            if (sub == null || email == null) {
                throw InvalidIdTokenException.of("Missing required claims: sub or email");
            }

            var emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));

            if (!emailVerified) {
                throw EmailNotVerifiedException.forEmail(email);
            }

            return SocialIdentity.builder()
                    .provider(provider)
                    .subject(sub)
                    .email(email)
                    .emailVerified(emailVerified)
                    .build();
        } catch (JwtException e) {
            throw InvalidIdTokenException.of(e.getMessage());
        }
    }
}
