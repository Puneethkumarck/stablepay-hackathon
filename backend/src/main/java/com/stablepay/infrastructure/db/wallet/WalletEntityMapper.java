package com.stablepay.infrastructure.db.wallet;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stablepay.domain.wallet.model.Wallet;

@Mapper
public interface WalletEntityMapper {
    Wallet toDomain(WalletEntity entity);

    @Mapping(target = "keyShareDek", ignore = true)
    @Mapping(target = "keyShareIv", ignore = true)
    @Mapping(target = "peerKeyShareIv", ignore = true)
    WalletEntity toEntity(Wallet domain);
}
