package com.stablepay.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record UserResponse(
    UUID id,
    String email,
    String name,
    Instant createdAt
) {}
