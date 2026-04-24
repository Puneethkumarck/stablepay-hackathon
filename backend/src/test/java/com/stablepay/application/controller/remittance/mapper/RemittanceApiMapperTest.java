package com.stablepay.application.controller.remittance.mapper;

import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.application.dto.RemittanceResponse;
import com.stablepay.domain.remittance.model.RemittanceStatus;

class RemittanceApiMapperTest {

    private final RemittanceApiMapper mapper = new RemittanceApiMapperImpl();

    @Test
    void shouldMapRemittanceToResponse() {
        // given
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .escrowPda("EsCrOwPdA123")
                .build();

        // when
        var response = mapper.toResponse(remittance);

        // then
        var expected = RemittanceResponse.builder()
                .id(remittance.id())
                .remittanceId(remittance.remittanceId())
                .recipientPhone(remittance.recipientPhone())
                .amountUsdc(remittance.amountUsdc())
                .amountInr(remittance.amountInr())
                .fxRate(remittance.fxRate())
                .status(RemittanceStatus.ESCROWED)
                .escrowPda("EsCrOwPdA123")
                .smsNotificationFailed(false)
                .createdAt(remittance.createdAt())
                .updatedAt(remittance.updatedAt())
                .recipientName(remittance.recipientName())
                .build();

        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
