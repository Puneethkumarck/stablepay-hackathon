package com.stablepay.infrastructure.auth.jwt;

import org.springframework.stereotype.Component;

import com.stablepay.domain.auth.port.TokenHasher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class TokenHasherAdapter implements TokenHasher {

    private final RefreshTokenGenerator refreshTokenGenerator;

    @Override
    public String hash(String rawToken) {
        return refreshTokenGenerator.hash(rawToken);
    }
}
