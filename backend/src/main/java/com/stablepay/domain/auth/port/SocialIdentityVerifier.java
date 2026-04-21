package com.stablepay.domain.auth.port;

import com.stablepay.domain.auth.model.SocialIdentity;

public interface SocialIdentityVerifier {
    SocialIdentity verify(String provider, String idToken);
}
