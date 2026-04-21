package com.stablepay.domain.auth.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.exception.UserNotFoundException;
import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.model.AuthTokenConfig;
import com.stablepay.domain.auth.model.LoginResult;
import com.stablepay.domain.auth.model.RefreshToken;
import com.stablepay.domain.auth.port.AuthTokenIssuer;
import com.stablepay.domain.auth.port.RefreshTokenRepository;
import com.stablepay.domain.auth.port.SocialIdentityRepository;
import com.stablepay.domain.auth.port.SocialIdentityVerifier;
import com.stablepay.domain.auth.port.TokenHasher;
import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.handler.CreateWalletHandler;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SocialLoginHandler {

    private final SocialIdentityVerifier socialIdentityVerifier;
    private final SocialIdentityRepository socialIdentityRepository;
    private final UserRepository userRepository;
    private final CreateWalletHandler createWalletHandler;
    private final WalletRepository walletRepository;
    private final AuthTokenIssuer authTokenIssuer;
    private final TokenHasher tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenConfig authTokenConfig;
    private final Clock clock;

    public LoginResult handle(String provider, String idToken) {
        var now = Instant.now(clock);
        var identity = socialIdentityVerifier.verify(provider, idToken);

        var existingIdentity = socialIdentityRepository.findByProviderAndSubject(
                identity.provider(), identity.subject());

        var newUser = existingIdentity.isEmpty();
        var appUser = existingIdentity
                .map(existing -> userRepository.findById(existing.userId())
                        .orElseThrow(() -> UserNotFoundException.byId(existing.userId())))
                .orElseGet(() -> {
                    var user = AppUser.builder()
                            .id(UUID.randomUUID())
                            .email(identity.email())
                            .createdAt(now)
                            .build();
                    var savedUser = userRepository.save(user);
                    socialIdentityRepository.save(
                            identity.toBuilder().userId(savedUser.id()).build());
                    return savedUser;
                });

        var wallet = existingIdentity.isPresent()
                ? walletRepository.findByUserId(appUser.id())
                        .orElseThrow(() -> WalletNotFoundException.byUserId(appUser.id()))
                : createWalletHandler.handle(appUser.id());

        var session = authTokenIssuer.issue(appUser.id());
        var tokenHash = tokenHasher.hash(session.refreshToken());

        refreshTokenRepository.revokeByUserId(appUser.id());

        var refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(appUser.id())
                .tokenHash(tokenHash)
                .expiresAt(now.plus(authTokenConfig.refreshTtl()))
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("Social login completed for userId={}, newUser={}", appUser.id(), newUser);

        return LoginResult.builder()
                .session(session)
                .user(appUser)
                .wallet(wallet)
                .newUser(newUser)
                .build();
    }
}
