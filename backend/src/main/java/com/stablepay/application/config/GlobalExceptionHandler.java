package com.stablepay.application.config;

import java.time.Clock;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.domain.claim.exception.ClaimAlreadyClaimedException;
import com.stablepay.domain.claim.exception.ClaimTokenExpiredException;
import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.fx.exception.UnsupportedCorridorException;
import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.wallet.exception.InsufficientBalanceException;
import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(WalletNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleWalletNotFound(
            WalletNotFoundException ex, HttpServletRequest request) {
        log.warn("Wallet not found: {}", ex.getMessage());
        return buildResponse("SP-0006", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientBalance(
            InsufficientBalanceException ex, HttpServletRequest request) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return buildResponse("SP-0002", ex.getMessage(), request);
    }

    @ExceptionHandler(TreasuryDepletedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleTreasuryDepleted(
            TreasuryDepletedException ex, HttpServletRequest request) {
        log.warn("Treasury depleted: {}", ex.getMessage());
        return buildResponse("SP-0007", ex.getMessage(), request);
    }

    @ExceptionHandler(WalletAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleWalletAlreadyExists(
            WalletAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Wallet already exists: {}", ex.getMessage());
        return buildResponse("SP-0008", ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedCorridorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnsupportedCorridor(
            UnsupportedCorridorException ex, HttpServletRequest request) {
        log.warn("Unsupported corridor requested: {}", ex.getMessage());
        return buildResponse("SP-0009", ex.getMessage(), request);
    }

    @ExceptionHandler(ClaimTokenNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleClaimTokenNotFound(
            ClaimTokenNotFoundException ex, HttpServletRequest request) {
        log.warn("Claim token not found: {}", ex.getMessage());
        return buildResponse("SP-0011", ex.getMessage(), request);
    }

    @ExceptionHandler(ClaimAlreadyClaimedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleClaimAlreadyClaimed(
            ClaimAlreadyClaimedException ex, HttpServletRequest request) {
        log.warn("Claim already submitted: {}", ex.getMessage());
        return buildResponse("SP-0012", ex.getMessage(), request);
    }

    @ExceptionHandler(ClaimTokenExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ErrorResponse handleClaimTokenExpired(
            ClaimTokenExpiredException ex, HttpServletRequest request) {
        log.warn("Claim token expired: {}", ex.getMessage());
        return buildResponse("SP-0013", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRemittanceStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidRemittanceState(
            InvalidRemittanceStateException ex, HttpServletRequest request) {
        log.warn("Invalid remittance state: {}", ex.getMessage());
        return buildResponse("SP-0014", ex.getMessage(), request);
    }

    @ExceptionHandler(DisbursementException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleDisbursementFailure(
            DisbursementException ex, HttpServletRequest request) {
        log.error("Disbursement failed: {}", ex.getMessage());
        return buildResponse("SP-0018", ex.getMessage(), request);
    }

    @ExceptionHandler(RemittanceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleRemittanceNotFound(
            RemittanceNotFoundException ex, HttpServletRequest request) {
        log.warn("Remittance not found: {}", ex.getMessage());
        return buildResponse("SP-0010", ex.getMessage(), request);
    }

    @ExceptionHandler(FundingOrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleFundingOrderNotFound(
            FundingOrderNotFoundException ex, HttpServletRequest request) {
        log.warn("Funding order not found: {}", ex.getMessage());
        return buildResponse("SP-0020", ex.getMessage(), request);
    }

    @ExceptionHandler(FundingFailedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleFundingFailed(
            FundingFailedException ex, HttpServletRequest request) {
        log.error("Funding failed: {}", ex.getMessage());
        return buildResponse("SP-0021", ex.getMessage(), request);
    }

    @ExceptionHandler(FundingAlreadyInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFundingAlreadyInProgress(
            FundingAlreadyInProgressException ex, HttpServletRequest request) {
        log.warn("Funding already in progress: {}", ex.getMessage());
        return buildResponse("SP-0022", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        return buildResponse("SP-0003", message, request);
    }

    private ErrorResponse buildResponse(
            String errorCode, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now(clock))
                .path(request.getRequestURI())
                .build();
    }
}
