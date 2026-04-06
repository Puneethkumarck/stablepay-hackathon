package com.stablepay.infrastructure.stub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StubMpcWalletClientTest {

    private final StubMpcWalletClient stubClient = new StubMpcWalletClient();

    @Test
    void shouldGenerateKeyWithSolanaAddressAndKeyData() {
        // given — no special setup needed for stub

        // when
        var result = stubClient.generateKey();

        // then
        assertThat(result.solanaAddress()).isNotBlank();
        assertThat(result.solanaAddress()).hasSizeGreaterThanOrEqualTo(32);
        assertThat(result.publicKey()).hasSize(32);
        assertThat(result.keyShareData()).hasSize(64);
    }

    @Test
    void shouldGenerateUniqueAddressesForConsecutiveCalls() {
        // given — no special setup needed for stub

        // when
        var first = stubClient.generateKey();
        var second = stubClient.generateKey();

        // then
        assertThat(first.solanaAddress()).isNotEqualTo(second.solanaAddress());
    }

    @Test
    void shouldSignTransactionAndReturnSignature() {
        // given
        var transactionBytes = new byte[]{10, 20, 30};
        var keyShareData = new byte[]{40, 50, 60};

        // when
        var result = stubClient.signTransaction(transactionBytes, keyShareData);

        // then
        assertThat(result).hasSize(64);
    }
}
