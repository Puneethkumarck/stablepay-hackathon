package com.stablepay.infrastructure.db.claim;

import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.claim.model.ClaimToken;
import com.stablepay.domain.claim.port.ClaimTokenRepository;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.test.PgTest;

@PgTest
@Transactional
class ClaimTokenRepositoryIntegrationTest {

    @Autowired
    private ClaimTokenRepository claimTokenRepository;

    @Autowired
    private RemittanceRepository remittanceRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID createUser() {
        var userId = UUID.randomUUID();
        userRepository.save(AppUser.builder().id(userId).email(userId + "@test.com").build());
        return userId;
    }

    private UUID createRemittanceAndReturnId() {
        var senderId = createUser();
        var remittanceId = UUID.randomUUID();
        var remittance = Remittance.builder()
                .remittanceId(remittanceId)
                .senderId(senderId)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .build();
        remittanceRepository.save(remittance);
        return remittanceId;
    }

    @Nested
    class Save {

        @Test
        void shouldSaveClaimTokenAndAssignId() {
            // given
            var remittanceId = createRemittanceAndReturnId();
            var claimToken = ClaimToken.builder()
                    .remittanceId(remittanceId)
                    .token("claim-save-" + System.nanoTime())
                    .claimed(false)
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();

            // when
            var saved = claimTokenRepository.save(claimToken);

            // then
            assertThat(saved.id()).isNotNull();
            assertThat(saved)
                    .usingRecursiveComparison()
                    .ignoringFields("id", "createdAt")
                    .isEqualTo(claimToken);
        }

        @Test
        void shouldPersistUpiIdWhenProvided() {
            // given
            var remittanceId = createRemittanceAndReturnId();
            var claimToken = ClaimToken.builder()
                    .remittanceId(remittanceId)
                    .token("claim-upi-" + System.nanoTime())
                    .claimed(true)
                    .upiId("recipient@upi")
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();

            // when
            var saved = claimTokenRepository.save(claimToken);
            var found = claimTokenRepository.findByToken(saved.token());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().upiId()).isEqualTo("recipient@upi");
            assertThat(found.get().claimed()).isTrue();
        }
    }

    @Nested
    class FindByToken {

        @Test
        void shouldFindClaimTokenByTokenString() {
            // given
            var remittanceId = createRemittanceAndReturnId();
            var tokenStr = "claim-find-" + System.nanoTime();
            var claimToken = ClaimToken.builder()
                    .remittanceId(remittanceId)
                    .token(tokenStr)
                    .claimed(false)
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();
            var saved = claimTokenRepository.save(claimToken);

            // when
            var found = claimTokenRepository.findByToken(tokenStr);

            // then
            assertThat(found).isPresent();
            assertThat(found.get())
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(saved);
        }

        @Test
        void shouldReturnEmptyWhenTokenNotFound() {
            // when
            var found = claimTokenRepository.findByToken("nonexistent-token");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    class ForeignKeyConstraint {

        @Test
        void shouldReferenceExistingRemittance() {
            // given
            var remittanceId = createRemittanceAndReturnId();
            var claimToken = ClaimToken.builder()
                    .remittanceId(remittanceId)
                    .token("claim-fk-" + System.nanoTime())
                    .claimed(false)
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();

            // when
            var saved = claimTokenRepository.save(claimToken);

            // then
            assertThat(saved.remittanceId()).isEqualTo(remittanceId);
        }
    }
}
