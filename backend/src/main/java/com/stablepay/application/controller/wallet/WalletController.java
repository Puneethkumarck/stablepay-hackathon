package com.stablepay.application.controller.wallet;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.wallet.mapper.WalletApiMapper;
import com.stablepay.application.dto.CreateWalletRequest;
import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.application.dto.FundWalletRequest;
import com.stablepay.application.dto.WalletResponse;
import com.stablepay.domain.wallet.handler.CreateWalletHandler;
import com.stablepay.domain.wallet.handler.FundWalletHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "MPC wallet creation and demo treasury funding")
public class WalletController {

    private final CreateWalletHandler createWalletHandler;
    private final FundWalletHandler fundWalletHandler;
    private final WalletApiMapper walletApiMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create wallet", description = "Creates an MPC-backed Solana wallet for the given user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Wallet created"),
        @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Wallet already exists for this user",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
        var wallet = createWalletHandler.handle(request.userId());
        return walletApiMapper.toResponse(wallet);
    }

    @PostMapping("/{id}/fund")
    @Operation(summary = "Fund wallet", description = "Adds demo USDC from the treasury to the wallet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet funded"),
        @ApiResponse(responseCode = "404", description = "Wallet not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Treasury depleted",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public WalletResponse fundWallet(
            @PathVariable Long id,
            @Valid @RequestBody FundWalletRequest request) {
        var wallet = fundWalletHandler.handle(id, request.amount());
        return walletApiMapper.toResponse(wallet);
    }
}
