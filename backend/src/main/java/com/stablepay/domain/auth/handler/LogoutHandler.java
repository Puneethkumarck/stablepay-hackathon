package com.stablepay.domain.auth.handler;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.port.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LogoutHandler {

    private final RefreshTokenRepository refreshTokenRepository;

    public void handle(UUID userId) {
        refreshTokenRepository.revokeByUserId(userId);
        log.info("All refresh tokens revoked for userId={}", userId);
    }
}
