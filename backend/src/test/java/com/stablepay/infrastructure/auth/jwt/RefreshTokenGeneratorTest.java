package com.stablepay.infrastructure.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenGeneratorTest {

    private RefreshTokenGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RefreshTokenGenerator();
    }

    @Test
    void shouldGenerateTokenWithR1Prefix() {
        // given

        // when
        var result = generator.generate();

        // then
        assertThat(result).startsWith("r1_");
    }

    @Test
    void shouldGenerateUrlSafeBase64Token() {
        // given

        // when
        var result = generator.generate();

        // then
        var tokenBody = result.substring(3);
        assertThat(tokenBody).doesNotContain("+", "=", "/");
    }

    @Test
    void shouldGenerateUniqueTokensOnEachCall() {
        // given

        // when
        var first = generator.generate();
        var second = generator.generate();

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldProduceConsistentHashForSameToken() {
        // given
        var token = generator.generate();

        // when
        var hash1 = generator.hash(token);
        var hash2 = generator.hash(token);

        // then
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void shouldProduceHexHashOf64Characters() {
        // given
        var token = generator.generate();

        // when
        var result = generator.hash(token);

        // then
        assertThat(result).hasSize(64);
        assertThat(result).matches("[0-9a-f]{64}");
    }
}
