package com.stablepay.infrastructure.persistence;

import org.mapstruct.Mapper;

import com.stablepay.domain.model.Remittance;

@Mapper(componentModel = "spring")
public interface RemittanceEntityMapper {
    Remittance toDomain(RemittanceEntity entity);
    RemittanceEntity toEntity(Remittance domain);
}
