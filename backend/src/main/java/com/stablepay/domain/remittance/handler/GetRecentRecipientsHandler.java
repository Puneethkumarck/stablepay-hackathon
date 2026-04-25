package com.stablepay.domain.remittance.handler;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.remittance.model.RecentRecipient;
import com.stablepay.domain.remittance.port.RemittanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecentRecipientsHandler {

    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 10;

    private final RemittanceRepository remittanceRepository;

    public List<RecentRecipient> handle(UUID senderId, int limit) {
        var effectiveLimit = Math.clamp(limit, 1, MAX_LIMIT);
        return remittanceRepository.findRecentRecipients(senderId, effectiveLimit);
    }
}
