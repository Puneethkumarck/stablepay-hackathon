package com.stablepay.infrastructure.db.remittance;

import org.mapstruct.Mapper;

import com.stablepay.domain.remittance.model.Remittance;

@Mapper(componentModel = "spring")
public interface RemittanceEntityMapper {
    Remittance toDomain(RemittanceEntity entity);
    RemittanceEntity toEntity(Remittance domain);
}
