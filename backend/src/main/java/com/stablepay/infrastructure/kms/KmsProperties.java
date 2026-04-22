package com.stablepay.infrastructure.kms;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.kms")
@Builder(toBuilder = true)
public record KmsProperties(
    @NotBlank String keyArn,
    String endpoint,
    String region
) {}
