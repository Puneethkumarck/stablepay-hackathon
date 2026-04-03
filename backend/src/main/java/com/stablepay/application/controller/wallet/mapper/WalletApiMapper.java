package com.stablepay.application.controller.wallet.mapper;

import org.mapstruct.Mapper;

import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.wallet.model.Wallet;

@Mapper(componentModel = "spring")
public interface WalletApiMapper {
    WalletResponse toResponse(Wallet wallet);
}
