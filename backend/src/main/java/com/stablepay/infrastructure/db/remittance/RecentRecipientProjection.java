package com.stablepay.infrastructure.db.remittance;

import java.time.Instant;

interface RecentRecipientProjection {
    String getName();
    String getPhone();
    Instant getLastSentAt();
}
