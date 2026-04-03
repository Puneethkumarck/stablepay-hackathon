package com.stablepay.domain.model;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Corridor {

    USD_INR("USD", "INR");

    private final String fromCurrency;
    private final String toCurrency;

    public static Optional<Corridor> find(String from, String to) {
        return Arrays.stream(values())
                .filter(c -> c.fromCurrency.equalsIgnoreCase(from) && c.toCurrency.equalsIgnoreCase(to))
                .findFirst();
    }
}
