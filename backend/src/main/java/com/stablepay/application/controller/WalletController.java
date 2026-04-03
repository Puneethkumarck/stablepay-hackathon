package com.stablepay.application.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.dto.CreateWalletRequest;
import com.stablepay.application.dto.FundWalletRequest;
import com.stablepay.application.dto.WalletResponse;
import com.stablepay.application.mapper.WalletApiMapper;
import com.stablepay.domain.port.inbound.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletApiMapper walletApiMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
        var wallet = walletService.create(request.userId());
        return walletApiMapper.toResponse(wallet);
    }

    @PostMapping("/{id}/fund")
    public WalletResponse fundWallet(@PathVariable Long id, @Valid @RequestBody FundWalletRequest request) {
        var wallet = walletService.fund(id, request.amount());
        return walletApiMapper.toResponse(wallet);
    }
}
