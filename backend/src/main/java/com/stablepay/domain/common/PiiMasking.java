package com.stablepay.domain.common;

public final class PiiMasking {

    private PiiMasking() {}

    public static String maskUpi(String upiId) {
        if (upiId == null || upiId.length() <= 4) {
            return "****";
        }
        return upiId.substring(0, 3) + "****";
    }
}
