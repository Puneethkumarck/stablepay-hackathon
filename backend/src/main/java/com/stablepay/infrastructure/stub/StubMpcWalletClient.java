package com.stablepay.infrastructure.stub;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.stablepay.domain.wallet.port.MpcWalletClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("stub")
public class StubMpcWalletClient implements MpcWalletClient {

    private static final String STUB_ADDRESS_PREFIX = "StubSoL";
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String generateKey() {
        var index = counter.incrementAndGet();
        var address = generateDeterministicAddress(index);

        log.info("STUB: Generated MPC wallet #{} with address={}", index, address);
        return address;
    }

    @Override
    public byte[] signTransaction(byte[] transactionBytes, byte[] keyShareData) {
        var signature = generateStubBytes("sig-" + System.nanoTime(), 64);
        log.info("STUB: Signed transaction ({} bytes)", transactionBytes.length);
        return signature;
    }

    private String generateDeterministicAddress(int index) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest((STUB_ADDRESS_PREFIX + index).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 43);
        } catch (NoSuchAlgorithmException e) {
            return STUB_ADDRESS_PREFIX + String.format("%036d", index);
        }
    }

    private byte[] generateStubBytes(String seed, int length) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            var result = new byte[length];
            System.arraycopy(hash, 0, result, 0, Math.min(hash.length, length));
            return result;
        } catch (NoSuchAlgorithmException e) {
            return new byte[length];
        }
    }
}
