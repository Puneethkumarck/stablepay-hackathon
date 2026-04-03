package com.stablepay.infrastructure.persistence;

import org.mapstruct.Mapper;

import com.stablepay.domain.model.ClaimToken;

@Mapper(componentModel = "spring")
public interface ClaimTokenEntityMapper {
    ClaimToken toDomain(ClaimTokenEntity entity);
    ClaimTokenEntity toEntity(ClaimToken domain);
}
