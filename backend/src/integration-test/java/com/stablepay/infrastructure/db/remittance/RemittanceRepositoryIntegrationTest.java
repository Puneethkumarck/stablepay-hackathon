package com.stablepay.infrastructure.db.remittance;

import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.test.PgTest;
import com.stablepay.testutil.AuthFixtures;

@PgTest
@Transactional
class RemittanceRepositoryIntegrationTest {

    @Autowired
    private RemittanceRepository remittanceRepository;

    @Autowired
    private UserRepository userRepository;

    private Remittance buildRemittance(UUID remittanceId, UUID senderId) {
        return Remittance.builder()
                .remittanceId(remittanceId)
                .senderId(senderId)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .build();
    }

    @Nested
    class Save {

        @Test
        void shouldSaveAndAssignId() {
            // given
            var senderId = AuthFixtures.createTestUser(userRepository);
            var remittance = buildRemittance(UUID.randomUUID(), senderId);

            // when
            var saved = remittanceRepository.save(remittance);

            // then
            assertThat(saved.id()).isNotNull();
            assertThat(saved)
                    .usingRecursiveComparison()
                    .ignoringFields("id", "createdAt", "updatedAt")
                    .isEqualTo(remittance);
        }

        @Test
        void shouldPersistAllFieldsCorrectly() {
            // given
            var senderId = AuthFixtures.createTestUser(userRepository);
            var remittanceId = UUID.randomUUID();
            var remittance = buildRemittance(remittanceId, senderId)
                    .toBuilder()
                    .escrowPda("EsCrOwPdA1234567890AbCdEfGh")
                    .claimTokenId("claim-token-123")
                    .smsNotificationFailed(true)
                    .build();

            // when
            var saved = remittanceRepository.save(remittance);
            var found = remittanceRepository.findByRemittanceId(remittanceId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get())
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt", "updatedAt")
                    .isEqualTo(saved);
        }
    }

    @Nested
    class FindByRemittanceId {

        @Test
        void shouldFindRemittanceByUuid() {
            // given
            var senderId = AuthFixtures.createTestUser(userRepository);
            var remittanceId = UUID.randomUUID();
            var remittance = buildRemittance(remittanceId, senderId);
            remittanceRepository.save(remittance);

            // when
            var found = remittanceRepository.findByRemittanceId(remittanceId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().remittanceId()).isEqualTo(remittanceId);
            assertThat(found.get().senderId()).isEqualTo(senderId);
        }

        @Test
        void shouldReturnEmptyWhenRemittanceIdNotFound() {
            // when
            var found = remittanceRepository.findByRemittanceId(UUID.randomUUID());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    class FindBySenderId {

        @Test
        void shouldReturnPagedResultsForSender() {
            // given
            var senderId = AuthFixtures.createTestUser(userRepository);
            remittanceRepository.save(buildRemittance(UUID.randomUUID(), senderId));
            remittanceRepository.save(buildRemittance(UUID.randomUUID(), senderId));
            remittanceRepository.save(buildRemittance(UUID.randomUUID(), senderId));

            // when
            var page = remittanceRepository.findBySenderId(senderId, PageRequest.of(0, 2));

            // then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        void shouldReturnEmptyPageWhenNoRemittancesForSender() {
            // when
            var page = remittanceRepository.findBySenderId(
                    UUID.randomUUID(), PageRequest.of(0, 10));

            // then
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        void shouldNotReturnRemittancesFromOtherSenders() {
            // given
            var senderId = AuthFixtures.createTestUser(userRepository);
            var otherSenderId = AuthFixtures.createTestUser(userRepository);
            remittanceRepository.save(buildRemittance(UUID.randomUUID(), senderId));
            remittanceRepository.save(buildRemittance(UUID.randomUUID(), otherSenderId));

            // when
            var page = remittanceRepository.findBySenderId(senderId, PageRequest.of(0, 10));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().getFirst().senderId()).isEqualTo(senderId);
        }
    }

    @Nested
    class StatusPersistence {

        @Test
        void shouldPersistStatusUpdatesCorrectly() {
            // given
            var senderId = AuthFixtures.createTestUser(userRepository);
            var remittanceId = UUID.randomUUID();
            var remittance = buildRemittance(remittanceId, senderId);
            var saved = remittanceRepository.save(remittance);

            // when
            var updated = remittanceRepository.save(
                    saved.toBuilder().status(RemittanceStatus.ESCROWED).build());

            // then
            var found = remittanceRepository.findByRemittanceId(remittanceId);
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo(RemittanceStatus.ESCROWED);
        }
    }
}
