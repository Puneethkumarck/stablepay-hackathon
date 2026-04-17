package com.stablepay.infrastructure.db.funding;

import org.mapstruct.Mapper;

import com.stablepay.domain.funding.model.FundingOrder;

@Mapper
public interface FundingOrderEntityMapper {
    FundingOrder toDomain(FundingOrderEntity entity);
    FundingOrderEntity toEntity(FundingOrder domain);
}
