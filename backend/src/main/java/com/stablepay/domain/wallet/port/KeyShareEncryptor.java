package com.stablepay.domain.wallet.port;

import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;

public interface KeyShareEncryptor {
    EncryptedKeyMaterial encrypt(byte[] keyShareData, byte[] peerKeyShareData);

    DecryptedKeyMaterial decrypt(byte[] encKeyShare, byte[] encPeerKeyShare,
                                 byte[] encDek, byte[] iv, byte[] peerIv);
}
