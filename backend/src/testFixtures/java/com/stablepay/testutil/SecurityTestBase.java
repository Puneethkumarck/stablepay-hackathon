package com.stablepay.testutil;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public final class SecurityTestBase {

    private SecurityTestBase() {}

    private static final SecretKey HMAC_KEY = new SecretKeySpec(
            AuthFixtures.SOME_JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    public static RequestPostProcessor asUser(UUID userId) {
        return asUser(userId, Clock.systemUTC());
    }

    public static RequestPostProcessor asUser(UUID userId, Clock clock) {
        return (MockHttpServletRequest request) -> {
            request.addHeader("Authorization", "Bearer " + signedJwt(userId, clock));
            return request;
        };
    }

    public static String signedJwt(UUID userId) {
        return signedJwt(userId, Clock.systemUTC());
    }

    public static String signedJwt(UUID userId, Clock clock) {
        var now = Instant.now(clock);
        var claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                .build();

        var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJwt.sign(new MACSigner(HMAC_KEY));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign test JWT", e);
        }
        return signedJwt.serialize();
    }
}
