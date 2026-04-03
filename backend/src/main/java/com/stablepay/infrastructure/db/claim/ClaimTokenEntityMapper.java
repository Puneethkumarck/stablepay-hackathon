package com.stablepay.infrastructure.db.claim;

import org.mapstruct.Mapper;

import com.stablepay.domain.claim.model.ClaimToken;

@Mapper(componentModel = "spring")
public interface ClaimTokenEntityMapper {
    ClaimToken toDomain(ClaimTokenEntity entity);
    ClaimTokenEntity toEntity(ClaimToken domain);
}
