package com.stablepay.application.controller.recipient;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.recipient.mapper.RecipientApiMapper;
import com.stablepay.application.dto.RecentRecipientResponse;
import com.stablepay.domain.auth.model.AuthPrincipal;
import com.stablepay.domain.remittance.handler.GetRecentRecipientsHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recipients")
@RequiredArgsConstructor
@Tag(name = "Recipients", description = "Recipient lookup and history")
public class RecipientController {

    private final GetRecentRecipientsHandler getRecentRecipientsHandler;
    private final RecipientApiMapper recipientApiMapper;

    @GetMapping("/recent")
    @Operation(summary = "Recent recipients", description = "Returns distinct past recipients sorted by most recent transfer")
    @ApiResponse(responseCode = "200", description = "Recipients retrieved")
    public List<RecentRecipientResponse> getRecentRecipients(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        return getRecentRecipientsHandler.handle(principal.id(), limit).stream()
                .map(recipientApiMapper::toResponse)
                .toList();
    }
}
