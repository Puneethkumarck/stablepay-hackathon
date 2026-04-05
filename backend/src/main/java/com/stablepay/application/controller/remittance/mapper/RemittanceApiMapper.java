package com.stablepay.application.controller.remittance.mapper;

import org.mapstruct.Mapper;

import com.stablepay.application.dto.RemittanceResponse;
import com.stablepay.domain.remittance.model.Remittance;

@Mapper(componentModel = "spring")
public interface RemittanceApiMapper {
    RemittanceResponse toResponse(Remittance remittance);
}
