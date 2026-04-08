package com.stablepay.infrastructure.transak;

import lombok.Builder;

@Builder(toBuilder = true)
record TransakQuoteResponse(String quoteId, String fiatAmount, String cryptoAmount) {}
