package com.stablepay.domain.port.outbound;

public interface MpcWalletClient {
    String generateKey();
    byte[] signTransaction(byte[] transactionBytes, byte[] keyShareData);
}
