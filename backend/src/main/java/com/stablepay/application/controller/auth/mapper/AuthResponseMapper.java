package com.stablepay.application.controller.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stablepay.application.dto.AuthResponse;
import com.stablepay.application.dto.UserResponse;
import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.model.AuthSession;
import com.stablepay.domain.auth.model.LoginResult;
import com.stablepay.domain.wallet.model.Wallet;

@Mapper
public interface AuthResponseMapper {

    @Mapping(target = "accessToken", source = "result.session.accessToken")
    @Mapping(target = "refreshToken", source = "result.session.refreshToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "user", source = "result.user")
    @Mapping(target = "wallet", source = "result.wallet")
    AuthResponse toResponse(LoginResult result, int expiresIn);

    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "wallet", ignore = true)
    AuthResponse toRefreshResponse(AuthSession session, int expiresIn);

    UserResponse toUserResponse(AppUser user);

    WalletResponse toWalletResponse(Wallet wallet);
}
