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
import com.stablepay.application.dto.SubmitClaimRequest;
import com.stablepay.domain.claim.handler.GetClaimQueryHandler;
import com.stablepay.domain.claim.handler.SubmitClaimHandler;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final GetClaimQueryHandler getClaimQueryHandler;
    private final SubmitClaimHandler submitClaimHandler;
    private final ClaimApiMapper claimApiMapper;

    @GetMapping("/{token}")
    public ClaimResponse getClaim(@PathVariable String token) {
        var claimDetails = getClaimQueryHandler.handle(token);
        return claimApiMapper.toResponse(claimDetails);
    }

    @PostMapping("/{token}")
    public ClaimResponse submitClaim(
            @PathVariable String token,
            @Valid @RequestBody SubmitClaimRequest request) {
        var claimDetails = submitClaimHandler.handle(token, request.upiId());
        return claimApiMapper.toResponse(claimDetails);
    }
}
