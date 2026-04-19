package com.stablepay.domain.common;

import java.util.regex.Pattern;

public final class PiiMasking {

    private static final Pattern UPI_HANDLE = Pattern.compile("\\S+@\\S+");

    private PiiMasking() {}

    public static String maskUpi(String upiId) {
        if (upiId == null || upiId.length() <= 4) {
            return "****";
        }
        return upiId.substring(0, 3) + "****";
    }

    public static String maskUpiSubstrings(String input) {
        if (input == null) {
            return null;
        }
        return UPI_HANDLE.matcher(input).replaceAll(match -> maskUpi(match.group()));
    }
}
