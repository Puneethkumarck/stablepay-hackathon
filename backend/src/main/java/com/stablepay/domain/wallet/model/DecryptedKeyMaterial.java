package com.stablepay.domain.wallet.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record DecryptedKeyMaterial(
    byte[] keyShareData,
    byte[] peerKeyShareData
) {
    public DecryptedKeyMaterial {
        keyShareData = keyShareData != null ? keyShareData.clone() : null;
        peerKeyShareData = peerKeyShareData != null ? peerKeyShareData.clone() : null;
    }
}
