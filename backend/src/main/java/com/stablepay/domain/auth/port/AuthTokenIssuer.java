package com.stablepay.domain.auth.port;

import java.util.UUID;

import com.stablepay.domain.auth.model.AuthSession;

public interface AuthTokenIssuer {
    AuthSession issue(UUID userId);
}
