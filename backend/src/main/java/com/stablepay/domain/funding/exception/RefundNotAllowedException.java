package com.stablepay.domain.funding.exception;

import com.stablepay.domain.funding.model.FundingStatus;

public class RefundNotAllowedException extends RuntimeException {

    public static RefundNotAllowedException forStatus(FundingStatus status) {
        return new RefundNotAllowedException(
                "SP-0023: Refund not allowed for funding order in status: " + status);
    }

    private RefundNotAllowedException(String message) {
        super(message);
    }
}
