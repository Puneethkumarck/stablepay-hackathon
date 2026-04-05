package com.stablepay.application.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.domain.claim.exception.ClaimAlreadyClaimedException;
import com.stablepay.domain.claim.exception.ClaimTokenExpiredException;
import com.stablepay.domain.claim.exception.ClaimTokenNotFoundException;
import com.stablepay.domain.fx.exception.UnsupportedCorridorException;
import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.wallet.exception.InsufficientBalanceException;
import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleWalletNotFound(WalletNotFoundException ex) {
        log.warn("Wallet not found: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0006")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0002")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(TreasuryDepletedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleTreasuryDepleted(TreasuryDepletedException ex) {
        log.warn("Treasury depleted: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0007")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(WalletAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleWalletAlreadyExists(WalletAlreadyExistsException ex) {
        log.warn("Wallet already exists: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0008")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(UnsupportedCorridorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnsupportedCorridor(UnsupportedCorridorException ex) {
        log.warn("Unsupported corridor requested: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0009")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(ClaimTokenNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleClaimTokenNotFound(ClaimTokenNotFoundException ex) {
        log.warn("Claim token not found: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0011")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(ClaimAlreadyClaimedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleClaimAlreadyClaimed(ClaimAlreadyClaimedException ex) {
        log.warn("Claim already submitted: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0012")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(ClaimTokenExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ErrorResponse handleClaimTokenExpired(ClaimTokenExpiredException ex) {
        log.warn("Claim token expired: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0013")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(InvalidRemittanceStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidRemittanceState(InvalidRemittanceStateException ex) {
        log.warn("Invalid remittance state: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0014")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(RemittanceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleRemittanceNotFound(RemittanceNotFoundException ex) {
        log.warn("Remittance not found: {}", ex.getMessage());
        return ErrorResponse.builder()
                .errorCode("SP-0010")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        return ErrorResponse.builder()
                .errorCode("SP-0003")
                .message(message)
                .build();
    }
}
