package com.stablepay.application.mapper;

import org.mapstruct.Mapper;

import com.stablepay.application.dto.FxRateResponse;
import com.stablepay.domain.model.FxQuote;

@Mapper(componentModel = "spring")
public interface FxRateApiMapper {
    FxRateResponse toResponse(FxQuote quote);
}
