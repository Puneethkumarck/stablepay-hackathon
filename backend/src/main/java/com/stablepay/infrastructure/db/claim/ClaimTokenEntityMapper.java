package com.stablepay.infrastructure.db.claim;

import org.mapstruct.Mapper;

import com.stablepay.domain.claim.model.ClaimToken;

@Mapper
public interface ClaimTokenEntityMapper {
    ClaimToken toDomain(ClaimTokenEntity entity);
    ClaimTokenEntity toEntity(ClaimToken domain);
}
