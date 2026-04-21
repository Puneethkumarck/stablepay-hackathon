package com.stablepay.application.config;

import java.util.Collections;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.stablepay.domain.auth.model.AuthPrincipal;

@Component
public class AppUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var userId = UUID.fromString(jwt.getSubject());
        var principal = AuthPrincipal.builder().id(userId).build();
        return new JwtAuthenticationToken(jwt, Collections.emptyList(), principal.id().toString()) {
            @Override
            public Object getPrincipal() {
                return principal;
            }
        };
    }
}
