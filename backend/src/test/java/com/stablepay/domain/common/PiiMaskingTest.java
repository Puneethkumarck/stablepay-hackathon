package com.stablepay.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskingTest {

    @Test
    void shouldMaskUpiIdKeepingFirstThreeChars() {
        // given
        var upiId = "recipient@upi";

        // when
        var masked = PiiMasking.maskUpi(upiId);

        // then
        assertThat(masked).isEqualTo("rec****");
    }

    @Test
    void shouldReturnFullMaskWhenUpiIdIsNull() {
        // when
        var masked = PiiMasking.maskUpi(null);

        // then
        assertThat(masked).isEqualTo("****");
    }

    @Test
    void shouldReturnFullMaskWhenUpiIdIsShorterThanFiveChars() {
        // when
        var masked = PiiMasking.maskUpi("ab@x");

        // then
        assertThat(masked).isEqualTo("****");
    }

    @Test
    void shouldReturnFullMaskWhenUpiIdIsEmpty() {
        // when
        var masked = PiiMasking.maskUpi("");

        // then
        assertThat(masked).isEqualTo("****");
    }

    @Test
    void shouldMaskFiveCharUpiId() {
        // given
        var upiId = "ab@cd";

        // when
        var masked = PiiMasking.maskUpi(upiId);

        // then
        assertThat(masked).isEqualTo("ab@****");
    }
}
