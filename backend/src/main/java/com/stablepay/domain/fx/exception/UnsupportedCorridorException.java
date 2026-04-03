package com.stablepay.domain.fx.exception;

public class UnsupportedCorridorException extends RuntimeException {

    public static UnsupportedCorridorException forPair(String from, String to) {
        return new UnsupportedCorridorException(
                "SP-0009: Unsupported corridor: " + from + " -> " + to);
    }

    private UnsupportedCorridorException(String message) {
        super(message);
    }
}
