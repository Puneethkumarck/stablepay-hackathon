package com.stablepay.domain.remittance.port;

import java.util.List;
import java.util.UUID;

import com.stablepay.domain.remittance.model.RemittanceStatusEvent;

public interface RemittanceStatusEventRepository {
    RemittanceStatusEvent save(RemittanceStatusEvent event);
    List<RemittanceStatusEvent> findByRemittanceId(UUID remittanceId);
}
