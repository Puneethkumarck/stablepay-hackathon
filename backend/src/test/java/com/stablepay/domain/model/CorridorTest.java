package com.stablepay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorridorTest {

    @Test
    void shouldFindUsdInrCorridor() {
        // when
        var result = Corridor.find("USD", "INR");

        // then
        assertThat(result).isPresent().hasValue(Corridor.USD_INR);
    }

    @Test
    void shouldFindCorridorCaseInsensitively() {
        // when
        var result = Corridor.find("usd", "inr");

        // then
        assertThat(result).isPresent().hasValue(Corridor.USD_INR);
    }

    @Test
    void shouldReturnEmptyForUnsupportedCorridor() {
        // when
        var result = Corridor.find("EUR", "GBP");

        // then
        assertThat(result).isEmpty();
    }
}
