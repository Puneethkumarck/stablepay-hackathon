package com.stablepay.application.controller.funding;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.funding.mapper.FundingApiMapper;
import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.application.dto.FundWalletRequest;
import com.stablepay.application.dto.FundingOrderResponse;
import com.stablepay.domain.funding.handler.GetFundingOrderHandler;
import com.stablepay.domain.funding.handler.InitiateFundingHandler;
import com.stablepay.domain.funding.handler.RefundFundingHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Funding", description = "Stripe-backed wallet funding and status queries")
public class FundingController {

    private final InitiateFundingHandler initiateFundingHandler;
    private final GetFundingOrderHandler getFundingOrderHandler;
    private final RefundFundingHandler refundFundingHandler;
    private final FundingApiMapper fundingApiMapper;

    @PostMapping("/wallets/{id}/fund")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Fund wallet via Stripe",
            description = "Creates a Stripe PaymentIntent and a funding order. "
                    + "Returns the funding order reference plus a one-time Stripe client secret.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Funding order created"),
        @ApiResponse(responseCode = "400", description = "Amount validation failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Wallet not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Funding already in progress for this wallet",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "502", description = "Stripe PaymentIntent creation failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public FundingOrderResponse initiateFunding(
            @PathVariable Long id,
            @Valid @RequestBody FundWalletRequest request) {
        var result = initiateFundingHandler.handle(id, request.amount());
        return fundingApiMapper.toResponseWithClientSecret(result.order(), result.clientSecret());
    }

    @GetMapping("/funding-orders/{fundingId}")
    @Operation(summary = "Get funding order status",
            description = "Returns the current status of a funding order. "
                    + "The Stripe client secret is never returned here.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Funding order found"),
        @ApiResponse(responseCode = "404", description = "Funding order not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public FundingOrderResponse getFundingOrder(@PathVariable UUID fundingId) {
        var order = getFundingOrderHandler.handle(fundingId);
        return fundingApiMapper.toResponse(order);
    }

    @PostMapping("/funding-orders/{fundingId}/refund")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Refund a funded wallet",
            description = "Refunds a FUNDED order via Stripe. Rejects if the sender's on-chain USDC "
                    + "balance or wallet available balance is less than the funding amount. "
                    + "No on-chain USDC is returned — the sender's test tokens stay in their ATA.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund completed"),
        @ApiResponse(responseCode = "404", description = "Funding order not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Refund not allowed or balance insufficient",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "502", description = "Stripe refund failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public FundingOrderResponse refundFunding(@PathVariable UUID fundingId) {
        var order = refundFundingHandler.handle(fundingId);
        return fundingApiMapper.toResponse(order);
    }
}
