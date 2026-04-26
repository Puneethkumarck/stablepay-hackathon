package com.stablepay.application.controller.recipient.mapper;

import org.mapstruct.Mapper;

import com.stablepay.application.dto.RecentRecipientResponse;
import com.stablepay.domain.remittance.model.RecentRecipient;

@Mapper
public interface RecipientApiMapper {
    RecentRecipientResponse toResponse(RecentRecipient recentRecipient);
}
