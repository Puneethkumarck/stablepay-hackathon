package com.stablepay.application.controller.claim;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.claim.mapper.ClaimApiMapper;
import com.stablepay.application.dto.ClaimResponse;
import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.application.dto.SubmitClaimRequest;
import com.stablepay.domain.claim.handler.GetClaimQueryHandler;
import com.stablepay.domain.claim.handler.SubmitClaimHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Recipient claim page and submission")
public class ClaimController {

    private final GetClaimQueryHandler getClaimQueryHandler;
    private final SubmitClaimHandler submitClaimHandler;
    private final ClaimApiMapper claimApiMapper;

    @GetMapping("/{token}")
    @Operation(summary = "Get claim details", description = "Retrieves remittance claim details by the claim token sent via SMS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Claim details retrieved"),
        @ApiResponse(responseCode = "404", description = "Claim token not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "410", description = "Claim token expired",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ClaimResponse getClaim(@PathVariable String token) {
        var claimDetails = getClaimQueryHandler.handle(token);
        return claimApiMapper.toResponse(claimDetails);
    }

    @PostMapping("/{token}")
    @Operation(summary = "Submit claim", description = "Submits a claim with the recipient's UPI ID to receive the INR payout")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Claim submitted"),
        @ApiResponse(responseCode = "404", description = "Claim token not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Claim already submitted",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "410", description = "Claim token expired",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ClaimResponse submitClaim(
            @PathVariable String token,
            @Valid @RequestBody SubmitClaimRequest request) {
        var claimDetails = submitClaimHandler.handle(token, request.upiId());
        return claimApiMapper.toResponse(claimDetails);
    }
}
