package com.stablepay.application.controller.remittance;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.remittance.mapper.RemittanceApiMapper;
import com.stablepay.application.dto.CreateRemittanceRequest;
import com.stablepay.application.dto.RemittanceResponse;
import com.stablepay.domain.remittance.handler.CreateRemittanceHandler;
import com.stablepay.domain.remittance.handler.GetRemittanceQueryHandler;
import com.stablepay.domain.remittance.handler.ListRemittancesQueryHandler;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/remittances")
@RequiredArgsConstructor
public class RemittanceController {

    private final CreateRemittanceHandler createRemittanceHandler;
    private final GetRemittanceQueryHandler getRemittanceQueryHandler;
    private final ListRemittancesQueryHandler listRemittancesQueryHandler;
    private final RemittanceApiMapper remittanceApiMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemittanceResponse createRemittance(@Valid @RequestBody CreateRemittanceRequest request) {
        var remittance = createRemittanceHandler.handle(
                request.senderId(), request.recipientPhone(), request.amountUsdc());
        return remittanceApiMapper.toResponse(remittance);
    }

    @GetMapping("/{remittanceId}")
    public RemittanceResponse getRemittance(@PathVariable UUID remittanceId) {
        var remittance = getRemittanceQueryHandler.handle(remittanceId);
        return remittanceApiMapper.toResponse(remittance);
    }

    @GetMapping
    public Page<RemittanceResponse> listRemittances(
            @RequestParam String senderId,
            Pageable pageable) {
        return listRemittancesQueryHandler.handle(senderId, pageable)
                .map(remittanceApiMapper::toResponse);
    }
}
