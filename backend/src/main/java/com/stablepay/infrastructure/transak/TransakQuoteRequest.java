package com.stablepay.infrastructure.transak;

import lombok.Builder;

@Builder(toBuilder = true)
record TransakQuoteRequest(
    String cryptoCurrency,
    String fiatCurrency,
    String fiatAmount,
    String type
) {}
