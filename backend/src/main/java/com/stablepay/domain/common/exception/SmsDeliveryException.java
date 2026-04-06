package com.stablepay.domain.common.exception;

public class SmsDeliveryException extends RuntimeException {

    public static SmsDeliveryException forRecipient(String phoneNumber, Throwable cause) {
        return new SmsDeliveryException(
                "SP-0017: SMS delivery failed for recipient: " + maskPhone(phoneNumber),
                cause);
    }

    public static SmsDeliveryException forRecipient(String phoneNumber, String reason) {
        return new SmsDeliveryException(
                "SP-0017: SMS delivery failed for recipient: " + maskPhone(phoneNumber) + " - " + reason);
    }

    private SmsDeliveryException(String message) {
        super(message);
    }

    private SmsDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
