package com.stablepay.application.mapper;

import org.mapstruct.Mapper;

import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.model.Wallet;

@Mapper(componentModel = "spring")
public interface WalletApiMapper {
    WalletResponse toResponse(Wallet wallet);
}
