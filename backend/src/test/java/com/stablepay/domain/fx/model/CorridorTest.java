package com.stablepay.domain.fx.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorridorTest {

    @Test
    void shouldFindUsdInr() {
        // given
        var from = "USD";
        var to = "INR";

        // when
        var result = Corridor.find(from, to);

        // then
        assertThat(result).isPresent().hasValue(Corridor.USD_INR);
    }

    @Test
    void shouldFindCaseInsensitive() {
        // given
        var from = "usd";
        var to = "inr";

        // when
        var result = Corridor.find(from, to);

        // then
        assertThat(result).isPresent().hasValue(Corridor.USD_INR);
    }

    @Test
    void shouldReturnEmptyForUnsupported() {
        // given
        var from = "EUR";
        var to = "INR";

        // when
        var result = Corridor.find(from, to);

        // then
        assertThat(result).isEmpty();
    }
}
