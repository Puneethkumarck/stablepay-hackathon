package com.stablepay.domain.remittance.port;

import java.util.UUID;

public interface RemittanceClaimSignaler {

    void signalClaim(UUID remittanceId, String claimToken, String upiId);
}
