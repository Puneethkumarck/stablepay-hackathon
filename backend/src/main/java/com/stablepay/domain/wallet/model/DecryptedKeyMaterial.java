package com.stablepay.domain.wallet.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record DecryptedKeyMaterial(
    byte[] keyShareData,
    byte[] peerKeyShareData
) {}
