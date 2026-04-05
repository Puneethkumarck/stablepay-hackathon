package com.stablepay.domain.remittance.handler;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.port.RemittanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListRemittancesQueryHandler {

    private final RemittanceRepository remittanceRepository;

    public Page<Remittance> handle(String senderId, Pageable pageable) {
        return remittanceRepository.findBySenderId(senderId, pageable);
    }
}
