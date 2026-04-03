package com.stablepay.application.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record ErrorResponse(
    String errorCode,
    String message
) {}
