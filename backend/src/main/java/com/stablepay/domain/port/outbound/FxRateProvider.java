package com.stablepay.domain.port.outbound;

import com.stablepay.domain.model.FxQuote;

public interface FxRateProvider {
    FxQuote getRate(String fromCurrency, String toCurrency);
}
