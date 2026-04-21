package com.stablepay.domain.auth.port;

public interface TokenHasher {
    String hash(String rawToken);
}
