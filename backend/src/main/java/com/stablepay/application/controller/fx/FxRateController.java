package com.stablepay.application.controller.fx;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.fx.mapper.FxRateApiMapper;
import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.application.dto.FxRateResponse;
import com.stablepay.domain.fx.handler.GetFxRateQueryHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fx")
@RequiredArgsConstructor
@Tag(name = "FX Rate", description = "Foreign exchange rate lookup")
public class FxRateController {

    private final GetFxRateQueryHandler getFxRateQueryHandler;
    private final FxRateApiMapper fxRateApiMapper;

    @GetMapping("/{from}-{to}")
    @Operation(summary = "Get FX rate", description = "Returns the current exchange rate for the given currency pair (e.g. USD-INR)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "FX rate retrieved"),
        @ApiResponse(responseCode = "400", description = "Unsupported currency corridor",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public FxRateResponse getRate(@PathVariable String from, @PathVariable String to) {
        var quote = getFxRateQueryHandler.handle(from.toUpperCase(), to.toUpperCase());
        return fxRateApiMapper.toResponse(quote);
    }
}
