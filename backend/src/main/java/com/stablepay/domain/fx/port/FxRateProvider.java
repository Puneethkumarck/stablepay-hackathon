package com.stablepay.domain.fx.port;

import com.stablepay.domain.fx.model.FxQuote;

public interface FxRateProvider {
    FxQuote getRate(String fromCurrency, String toCurrency);
}
