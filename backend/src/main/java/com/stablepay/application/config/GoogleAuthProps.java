package com.stablepay.application.config;

import static java.util.Objects.requireNonNull;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.auth.google")
@Validated
@Builder(toBuilder = true)
public record GoogleAuthProps(
    @NotEmpty List<String> clientIds
) {
    public GoogleAuthProps {
        requireNonNull(clientIds, "clientIds cannot be null");
        clientIds = List.copyOf(clientIds);
    }
}
