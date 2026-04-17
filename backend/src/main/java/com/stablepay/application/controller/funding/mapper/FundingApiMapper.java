package com.stablepay.application.controller.funding.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stablepay.application.dto.FundingOrderResponse;
import com.stablepay.domain.funding.model.FundingOrder;

@Mapper(componentModel = "spring")
public interface FundingApiMapper {

    @Mapping(target = "stripeClientSecret", ignore = true)
    FundingOrderResponse toResponse(FundingOrder order);

    default FundingOrderResponse toResponseWithClientSecret(FundingOrder order, String clientSecret) {
        return toResponse(order).toBuilder()
                .stripeClientSecret(clientSecret)
                .build();
    }
}
