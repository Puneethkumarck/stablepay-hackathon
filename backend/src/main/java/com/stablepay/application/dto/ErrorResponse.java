package com.stablepay.application.dto;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record ErrorResponse(
    String errorCode,
    String message,
    Instant timestamp,
    String path
) {}
