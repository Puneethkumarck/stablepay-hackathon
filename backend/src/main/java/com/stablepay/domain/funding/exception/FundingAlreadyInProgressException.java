package com.stablepay.domain.funding.exception;

public class FundingAlreadyInProgressException extends RuntimeException {

    public static FundingAlreadyInProgressException forWallet(Long walletId) {
        return new FundingAlreadyInProgressException(
                "SP-0022: Funding already in progress for wallet: " + walletId);
    }

    private FundingAlreadyInProgressException(String message) {
        super(message);
    }
}
