package com.stablepay.domain.wallet.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record EncryptedKeyMaterial(
    byte[] encryptedKeyShareData,
    byte[] encryptedPeerKeyShareData,
    byte[] encryptedDek,
    byte[] keyShareIv,
    byte[] peerKeyShareIv
) {
    public EncryptedKeyMaterial {
        encryptedKeyShareData = encryptedKeyShareData != null ? encryptedKeyShareData.clone() : null;
        encryptedPeerKeyShareData = encryptedPeerKeyShareData != null ? encryptedPeerKeyShareData.clone() : null;
        encryptedDek = encryptedDek != null ? encryptedDek.clone() : null;
        keyShareIv = keyShareIv != null ? keyShareIv.clone() : null;
        peerKeyShareIv = peerKeyShareIv != null ? peerKeyShareIv.clone() : null;
    }
}
