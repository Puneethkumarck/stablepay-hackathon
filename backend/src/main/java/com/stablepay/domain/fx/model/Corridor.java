package com.stablepay.domain.fx.model;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Corridor {
    USD_INR("USD", "INR");

    private final String sourceCurrency;
    private final String targetCurrency;

    public static Optional<Corridor> find(String from, String to) {
        return Arrays.stream(values())
                .filter(c -> c.sourceCurrency.equalsIgnoreCase(from)
                        && c.targetCurrency.equalsIgnoreCase(to))
                .findFirst();
    }
}
