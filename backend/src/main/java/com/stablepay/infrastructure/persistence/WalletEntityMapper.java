package com.stablepay.infrastructure.persistence;

import org.mapstruct.Mapper;

import com.stablepay.domain.model.Wallet;

@Mapper(componentModel = "spring")
public interface WalletEntityMapper {
    Wallet toDomain(WalletEntity entity);
    WalletEntity toEntity(Wallet domain);
}
