package com.stablepay.infrastructure.db.remittance;

import org.mapstruct.Mapper;

import com.stablepay.domain.remittance.model.RemittanceStatusEvent;

@Mapper
interface RemittanceStatusEventEntityMapper {
    RemittanceStatusEvent toDomain(RemittanceStatusEventEntity entity);
    RemittanceStatusEventEntity toEntity(RemittanceStatusEvent domain);
}
