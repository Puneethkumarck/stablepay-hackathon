package com.stablepay.application.controller.claim.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stablepay.application.dto.ClaimResponse;
import com.stablepay.domain.claim.model.ClaimDetails;

@Mapper
public interface ClaimApiMapper {

    @Mapping(source = "remittance.remittanceId", target = "remittanceId")
    @Mapping(source = "senderDisplayName", target = "senderDisplayName")
    @Mapping(source = "remittance.amountUsdc", target = "amountUsdc")
    @Mapping(source = "remittance.amountInr", target = "amountInr")
    @Mapping(source = "remittance.fxRate", target = "fxRate")
    @Mapping(source = "remittance.status", target = "status")
    @Mapping(source = "claimToken.claimed", target = "claimed")
    @Mapping(source = "claimToken.expiresAt", target = "expiresAt")
    ClaimResponse toResponse(ClaimDetails claimDetails);
}
