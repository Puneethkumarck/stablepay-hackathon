package com.stablepay.application.controller.fx.mapper;

import org.mapstruct.Mapper;

import com.stablepay.application.dto.FxRateResponse;
import com.stablepay.domain.fx.model.FxQuote;

@Mapper(componentModel = "spring")
public interface FxRateApiMapper {
    FxRateResponse toResponse(FxQuote quote);
}
