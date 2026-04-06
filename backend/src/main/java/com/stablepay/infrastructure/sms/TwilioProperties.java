package com.stablepay.infrastructure.sms;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.twilio")
@Builder(toBuilder = true)
public record TwilioProperties(
    @NotBlank String accountSid,
    @NotBlank String authToken,
    @NotBlank String phoneNumber
) {}
