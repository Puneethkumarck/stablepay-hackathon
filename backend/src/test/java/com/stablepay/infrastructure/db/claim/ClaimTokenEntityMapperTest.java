package com.stablepay.infrastructure.db.claim;

import static com.stablepay.testutil.ClaimTokenFixtures.SOME_CLAIM_TOKEN_ID;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_EXPIRES_AT;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_TOKEN;
import static com.stablepay.testutil.ClaimTokenFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.claim.model.ClaimToken;

class ClaimTokenEntityMapperTest {

    private final ClaimTokenEntityMapper mapper = new ClaimTokenEntityMapperImpl();

    @Test
    void shouldMapDomainToEntityAndBack() {
        // given
        var domain = ClaimToken.builder()
                .id(SOME_CLAIM_TOKEN_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .token(SOME_TOKEN)
                .claimed(true)
                .upiId(SOME_UPI_ID)
                .createdAt(SOME_CREATED_AT)
                .expiresAt(SOME_EXPIRES_AT)
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
        var entity = ClaimTokenEntity.builder()
                .id(SOME_CLAIM_TOKEN_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .token(SOME_TOKEN)
                .claimed(false)
                .upiId(null)
                .createdAt(SOME_CREATED_AT)
                .expiresAt(SOME_EXPIRES_AT)
                .build();

        // when
        var domain = mapper.toDomain(entity);

        // then
        var expected = ClaimToken.builder()
                .id(SOME_CLAIM_TOKEN_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .token(SOME_TOKEN)
                .claimed(false)
                .upiId(null)
                .createdAt(SOME_CREATED_AT)
                .expiresAt(SOME_EXPIRES_AT)
                .build();

        assertThat(domain)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
