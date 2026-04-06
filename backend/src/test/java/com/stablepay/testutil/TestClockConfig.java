package com.stablepay.testutil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestClockConfig {

    public static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T12:00:00Z");

    @Bean
    public Clock clock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
