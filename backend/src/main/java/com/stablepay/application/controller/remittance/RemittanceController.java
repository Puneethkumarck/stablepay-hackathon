package com.stablepay.application.controller.remittance;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.remittance.mapper.RemittanceApiMapper;
import com.stablepay.application.dto.CreateRemittanceRequest;
import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.application.dto.RemittanceResponse;
import com.stablepay.domain.auth.model.AuthPrincipal;
import com.stablepay.domain.remittance.handler.CreateRemittanceHandler;
import com.stablepay.domain.remittance.handler.GetRemittanceQueryHandler;
import com.stablepay.domain.remittance.handler.ListRemittancesQueryHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/remittances")
@RequiredArgsConstructor
@Tag(name = "Remittances", description = "Cross-border remittance creation and tracking")
public class RemittanceController {

    private final CreateRemittanceHandler createRemittanceHandler;
    private final GetRemittanceQueryHandler getRemittanceQueryHandler;
    private final ListRemittancesQueryHandler listRemittancesQueryHandler;
    private final RemittanceApiMapper remittanceApiMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create remittance", description = "Initiates a new USD to INR remittance with locked FX rate")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Remittance created"),
        @ApiResponse(responseCode = "400", description = "Validation error or insufficient balance",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public RemittanceResponse createRemittance(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateRemittanceRequest request) {
        var remittance = createRemittanceHandler.handle(
                principal.id(), request.recipientPhone(), request.amountUsdc());
        return remittanceApiMapper.toResponse(remittance);
    }

    @GetMapping("/{remittanceId}")
    @Operation(summary = "Get remittance", description = "Retrieves a remittance by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Remittance found"),
        @ApiResponse(responseCode = "404", description = "Remittance not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public RemittanceResponse getRemittance(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID remittanceId) {
        var remittance = getRemittanceQueryHandler.handle(remittanceId, principal.id());
        return remittanceApiMapper.toResponse(remittance);
    }

    @GetMapping
    @Operation(summary = "List remittances", description = "Returns a paginated list of remittances for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Remittances retrieved")
    })
    public Page<RemittanceResponse> listRemittances(
            @AuthenticationPrincipal AuthPrincipal principal,
            Pageable pageable) {
        return listRemittancesQueryHandler.handle(principal.id(), pageable)
                .map(remittanceApiMapper::toResponse);
    }
}
