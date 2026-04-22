package com.stablepay.domain.wallet.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record EncryptedKeyMaterial(
    byte[] encryptedKeyShareData,
    byte[] encryptedPeerKeyShareData,
    byte[] encryptedDek,
    byte[] keyShareIv,
    byte[] peerKeyShareIv
) {}
