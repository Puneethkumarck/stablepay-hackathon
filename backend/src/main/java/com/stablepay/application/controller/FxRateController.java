package com.stablepay.application.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.dto.FxRateResponse;
import com.stablepay.application.mapper.FxRateApiMapper;
import com.stablepay.domain.handler.GetFxRateQueryHandler;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fx")
@RequiredArgsConstructor
public class FxRateController {

    private final GetFxRateQueryHandler getFxRateQueryHandler;
    private final FxRateApiMapper fxRateApiMapper;

    @GetMapping("/usd-inr")
    public FxRateResponse getUsdInrRate() {
        var quote = getFxRateQueryHandler.handle("USD", "INR");
        return fxRateApiMapper.toResponse(quote);
    }
}
