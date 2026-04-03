package com.stablepay.domain.fx.handler;

import org.springframework.stereotype.Service;

import com.stablepay.domain.fx.exception.UnsupportedCorridorException;
import com.stablepay.domain.fx.model.Corridor;
import com.stablepay.domain.fx.model.FxQuote;
import com.stablepay.domain.fx.port.FxRateProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetFxRateQueryHandler {

    private final FxRateProvider fxRateProvider;

    public FxQuote handle(String from, String to) {
        var corridor = Corridor.find(from, to)
                .orElseThrow(() -> UnsupportedCorridorException.forPair(from, to));

        log.info("Fetching FX rate for corridor {}", corridor);
        return fxRateProvider.getRate(corridor.getSourceCurrency(), corridor.getTargetCurrency());
    }
}
