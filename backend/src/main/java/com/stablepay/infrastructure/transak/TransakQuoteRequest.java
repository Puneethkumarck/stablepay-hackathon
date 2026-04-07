package com.stablepay.infrastructure.transak;

record TransakQuoteRequest(
    String cryptoCurrency,
    String fiatCurrency,
    String fiatAmount,
    String type
) {}
