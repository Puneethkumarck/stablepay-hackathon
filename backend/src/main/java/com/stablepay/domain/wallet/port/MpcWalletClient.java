package com.stablepay.domain.wallet.port;

public interface MpcWalletClient {
    String generateKey();
    byte[] signTransaction(byte[] transactionBytes, byte[] keyShareData);
}
