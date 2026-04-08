package com.stablepay.infrastructure.db.remittance;

import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_DB_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_UPDATED_AT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;

class RemittanceEntityMapperTest {

    private final RemittanceEntityMapper mapper = new RemittanceEntityMapperImpl();

    @Test
    void shouldMapDomainToEntityAndBack() {
        // given
        var domain = Remittance.builder()
                .id(SOME_REMITTANCE_DB_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.ESCROWED)
                .escrowPda("EsCrOwPdA123")
                .claimTokenId("claim-token-123")
                .smsNotificationFailed(true)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        // when
        var entity = mapper.toEntity(domain);
        var backToDomain = mapper.toDomain(entity);

        // then
        assertThat(backToDomain)
                .usingRecursiveComparison()
                .isEqualTo(domain);
    }

    @Test
    void shouldMapEntityToDomainWithAllFields() {
        // given
        var entity = RemittanceEntity.builder()
                .id(SOME_REMITTANCE_DB_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.CLAIMED)
                .escrowPda("EsCrOwPdA456")
                .claimTokenId("claim-token-456")
                .smsNotificationFailed(false)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        // when
        var domain = mapper.toDomain(entity);

        // then
        var expected = Remittance.builder()
                .id(SOME_REMITTANCE_DB_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.CLAIMED)
                .escrowPda("EsCrOwPdA456")
                .claimTokenId("claim-token-456")
                .smsNotificationFailed(false)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        assertThat(domain)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
