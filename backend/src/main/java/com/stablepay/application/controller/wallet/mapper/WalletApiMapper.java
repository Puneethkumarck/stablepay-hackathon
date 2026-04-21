package com.stablepay.application.controller.wallet.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.wallet.model.Wallet;

@Mapper
public interface WalletApiMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "solanaAddress", source = "solanaAddress")
    @Mapping(target = "availableBalance", source = "availableBalance")
    @Mapping(target = "totalBalance", source = "totalBalance")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    WalletResponse toResponse(Wallet wallet);
}
