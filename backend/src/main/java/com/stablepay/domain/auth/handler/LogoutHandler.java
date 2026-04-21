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

    public void handle(UUID userId, String ip, String userAgent) {
        refreshTokenRepository.revokeByUserId(userId);
        log.info("LOGOUT userId={} ip={} userAgent={}", userId, ip, userAgent);
    }
}
