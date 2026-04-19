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

    @Test
    void shouldMaskSingleUpiHandleEmbeddedInMessage() {
        // given
        var input = "vpa.address alice@hdfcbank is invalid";

        // when
        var result = PiiMasking.maskUpiSubstrings(input);

        // then
        assertThat(result).isEqualTo("vpa.address ali**** is invalid");
    }

    @Test
    void shouldMaskMultipleUpiHandlesEmbeddedInMessage() {
        // given
        var input = "contact alice@hdfcbank and bob@icici both rejected";

        // when
        var result = PiiMasking.maskUpiSubstrings(input);

        // then
        assertThat(result).isEqualTo("contact ali**** and bob**** both rejected");
    }

    @Test
    void shouldReturnInputUnchangedWhenNoUpiHandlePresent() {
        // given
        var input = "HTTP 500";

        // when
        var result = PiiMasking.maskUpiSubstrings(input);

        // then
        assertThat(result).isEqualTo("HTTP 500");
    }

    @Test
    void shouldReturnNullWhenMaskUpiSubstringsInputIsNull() {
        // when
        var result = PiiMasking.maskUpiSubstrings(null);

        // then
        assertThat(result).isNull();
    }
}
