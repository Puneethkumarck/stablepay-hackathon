package com.stablepay.domain.wallet.port;

import com.stablepay.domain.wallet.model.GeneratedKey;

public interface MpcWalletClient {
    GeneratedKey generateKey();
    byte[] signTransaction(byte[] transactionBytes, byte[] keyShareData, byte[] peerKeyShareData);
}
