package com.stablepay.infrastructure.db.wallet;

import org.mapstruct.Mapper;

import com.stablepay.domain.wallet.model.Wallet;

@Mapper(componentModel = "spring")
public interface WalletEntityMapper {
    Wallet toDomain(WalletEntity entity);
    WalletEntity toEntity(Wallet domain);
}
