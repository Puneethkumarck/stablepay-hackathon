package com.stablepay.domain.handler;

import org.springframework.stereotype.Service;

import com.stablepay.domain.exception.UnsupportedCorridorException;
import com.stablepay.domain.model.Corridor;
import com.stablepay.domain.model.FxQuote;
import com.stablepay.domain.port.outbound.FxRateProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetFxRateQueryHandler {

    private final FxRateProvider fxRateProvider;

    public FxQuote handle(String fromCurrency, String toCurrency) {
        var corridor = Corridor.find(fromCurrency, toCurrency)
                .orElseThrow(() -> UnsupportedCorridorException.forPair(fromCurrency, toCurrency));
        log.debug("Fetching FX rate for corridor {}", corridor);
        return fxRateProvider.getRate(corridor.getFromCurrency(), corridor.getToCurrency());
    }
}
