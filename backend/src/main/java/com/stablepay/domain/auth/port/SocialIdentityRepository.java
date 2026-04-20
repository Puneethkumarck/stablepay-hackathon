package com.stablepay.domain.auth.port;

import java.util.Optional;
import java.util.UUID;

import com.stablepay.domain.auth.model.SocialIdentity;

public interface SocialIdentityRepository {
    Optional<SocialIdentity> findByProviderAndSubject(String provider, String subject);
    SocialIdentity save(SocialIdentity identity, UUID userId);
}
