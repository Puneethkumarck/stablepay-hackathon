package com.stablepay.infrastructure.mpc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.stablepay.domain.wallet.exception.MpcKeyGenerationException;
import com.stablepay.test.PgTest;

import io.github.resilience4j.retry.RetryRegistry;

@PgTest
class MpcKeygenRetryConfigIntegrationTest {

    @Autowired
    private RetryRegistry retryRegistry;

    @Test
    void shouldRegisterMpcKeygenRetryWithTransientExceptionRetriedAndPermanentIgnored() {
        // given — guards application.yml contract: any change that weakens the retry
        // policy (fewer attempts, wrong exception classification) will fail this test.
        var retry = retryRegistry.retry("mpcKeygen");
        var config = retry.getRetryConfig();

        // when / then — instance registered
        assertThat(retry.getName()).isEqualTo("mpcKeygen");

        // max attempts honours the yml setting
        assertThat(config.getMaxAttempts()).isEqualTo(3);

        // Transient exceptions are retried; Permanent are not
        var transientException = MpcKeyGenerationException.peerShareMissing("ceremony-under-test");
        var permanentException = MpcKeyGenerationException.permanentFailure(
                "ceremony-under-test", "bad input");
        assertThat(config.getExceptionPredicate().test(transientException)).isTrue();
        assertThat(config.getExceptionPredicate().test(permanentException)).isFalse();
    }
}
