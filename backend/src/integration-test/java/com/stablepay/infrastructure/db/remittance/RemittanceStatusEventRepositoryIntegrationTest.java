package com.stablepay.infrastructure.db.remittance;

import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.auth.port.UserRepository;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.RemittanceStatusEvent;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.domain.remittance.port.RemittanceStatusEventRepository;
import com.stablepay.test.PgTest;
import com.stablepay.testutil.AuthFixtures;

@PgTest
@Transactional
class RemittanceStatusEventRepositoryIntegrationTest {

    @Autowired
    private RemittanceStatusEventRepository remittanceStatusEventRepository;

    @Autowired
    private RemittanceRepository remittanceRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID remittanceId;

    @BeforeEach
    void setUp() {
        var senderId = AuthFixtures.createTestUser(userRepository);
        remittanceId = UUID.randomUUID();
        remittanceRepository.save(Remittance.builder()
                .remittanceId(remittanceId)
                .senderId(senderId)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .build());
    }

    @Nested
    class Save {

        @Test
        void shouldSaveAndAssignId() {
            // given
            var event = RemittanceStatusEvent.builder()
                    .remittanceId(remittanceId)
                    .status(RemittanceStatus.INITIATED)
                    .message("Payment received")
                    .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                    .build();

            // when
            var saved = remittanceStatusEventRepository.save(event);

            // then
            assertThat(saved.id()).isNotNull();
            assertThat(saved)
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(event);
        }
    }

    @Nested
    class FindByRemittanceId {

        @Test
        void shouldReturnEventsOrderedByCreatedAtAndId() {
            // given
            var event1 = RemittanceStatusEvent.builder()
                    .remittanceId(remittanceId)
                    .status(RemittanceStatus.INITIATED)
                    .message("Payment received")
                    .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                    .build();

            var event2 = RemittanceStatusEvent.builder()
                    .remittanceId(remittanceId)
                    .status(RemittanceStatus.ESCROWED)
                    .message("Funds secured in escrow")
                    .createdAt(Instant.parse("2026-04-03T10:01:00Z"))
                    .build();

            var event3 = RemittanceStatusEvent.builder()
                    .remittanceId(remittanceId)
                    .status(RemittanceStatus.CLAIMED)
                    .message("Recipient claimed funds")
                    .createdAt(Instant.parse("2026-04-03T10:02:00Z"))
                    .build();

            remittanceStatusEventRepository.save(event1);
            remittanceStatusEventRepository.save(event2);
            remittanceStatusEventRepository.save(event3);

            // when
            var events = remittanceStatusEventRepository.findByRemittanceId(remittanceId);

            // then
            assertThat(events).hasSize(3);
            assertThat(events.get(0).status()).isEqualTo(RemittanceStatus.INITIATED);
            assertThat(events.get(1).status()).isEqualTo(RemittanceStatus.ESCROWED);
            assertThat(events.get(2).status()).isEqualTo(RemittanceStatus.CLAIMED);
        }

        @Test
        void shouldOrderByIdWhenCreatedAtIsTied() {
            // given
            var sameTimestamp = Instant.parse("2026-04-03T10:00:00Z");

            var saved1 = remittanceStatusEventRepository.save(RemittanceStatusEvent.builder()
                    .remittanceId(remittanceId)
                    .status(RemittanceStatus.INITIATED)
                    .message("First event")
                    .createdAt(sameTimestamp)
                    .build());

            var saved2 = remittanceStatusEventRepository.save(RemittanceStatusEvent.builder()
                    .remittanceId(remittanceId)
                    .status(RemittanceStatus.ESCROWED)
                    .message("Second event")
                    .createdAt(sameTimestamp)
                    .build());

            // when
            var events = remittanceStatusEventRepository.findByRemittanceId(remittanceId);

            // then
            assertThat(events).hasSize(2);
            assertThat(events.get(0).id()).isEqualTo(saved1.id());
            assertThat(events.get(1).id()).isEqualTo(saved2.id());
            assertThat(events.get(0).id()).isLessThan(events.get(1).id());
        }

        @Test
        void shouldReturnEmptyListWhenNoEventsExist() {
            // given
            var otherRemittanceId = UUID.randomUUID();

            // when
            var events = remittanceStatusEventRepository.findByRemittanceId(otherRemittanceId);

            // then
            assertThat(events).isEmpty();
        }

        @Test
        void shouldNotReturnEventsFromOtherRemittances() {
            // given
            var senderId = AuthFixtures.createTestUser(userRepository);
            var otherRemittanceId = UUID.randomUUID();
            remittanceRepository.save(Remittance.builder()
                    .remittanceId(otherRemittanceId)
                    .senderId(senderId)
                    .recipientPhone(SOME_RECIPIENT_PHONE)
                    .amountUsdc(SOME_AMOUNT_USDC)
                    .amountInr(SOME_AMOUNT_INR)
                    .fxRate(SOME_FX_RATE)
                    .status(RemittanceStatus.INITIATED)
                    .smsNotificationFailed(false)
                    .build());

            remittanceStatusEventRepository.save(RemittanceStatusEvent.builder()
                    .remittanceId(remittanceId)
                    .status(RemittanceStatus.INITIATED)
                    .message("Payment received")
                    .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                    .build());

            remittanceStatusEventRepository.save(RemittanceStatusEvent.builder()
                    .remittanceId(otherRemittanceId)
                    .status(RemittanceStatus.INITIATED)
                    .message("Other payment")
                    .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                    .build());

            // when
            var events = remittanceStatusEventRepository.findByRemittanceId(remittanceId);

            // then
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().remittanceId()).isEqualTo(remittanceId);
        }
    }
}
