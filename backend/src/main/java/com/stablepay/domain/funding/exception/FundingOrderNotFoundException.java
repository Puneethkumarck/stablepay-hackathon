package com.stablepay.domain.funding.exception;

import java.util.UUID;

public class FundingOrderNotFoundException extends RuntimeException {

    public static FundingOrderNotFoundException byFundingId(UUID fundingId) {
        return new FundingOrderNotFoundException("SP-0020: Funding order not found: " + fundingId);
    }

    private FundingOrderNotFoundException(String message) {
        super(message);
    }
}
