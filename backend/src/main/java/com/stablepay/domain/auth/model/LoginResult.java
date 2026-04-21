package com.stablepay.domain.auth.model;

import static java.util.Objects.requireNonNull;

import com.stablepay.domain.wallet.model.Wallet;

import lombok.Builder;

@Builder(toBuilder = true)
public record LoginResult(
    AuthSession session,
    AppUser user,
    Wallet wallet,
    boolean newUser
) {
    public LoginResult {
        requireNonNull(session, "session cannot be null");
        requireNonNull(user, "user cannot be null");
        requireNonNull(wallet, "wallet cannot be null");
    }
}
