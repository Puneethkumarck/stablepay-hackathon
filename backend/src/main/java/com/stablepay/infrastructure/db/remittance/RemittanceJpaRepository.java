package com.stablepay.infrastructure.db.remittance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RemittanceJpaRepository extends JpaRepository<RemittanceEntity, Long> {
    Optional<RemittanceEntity> findByRemittanceId(UUID remittanceId);
    Page<RemittanceEntity> findBySenderId(UUID senderId, Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT name, phone, lastSentAt FROM (
                SELECT DISTINCT ON (r.recipient_phone)
                       r.recipient_name AS name,
                       r.recipient_phone AS phone,
                       r.created_at AS lastSentAt
                FROM remittances r
                WHERE r.sender_id = :senderId
                  AND r.recipient_name IS NOT NULL
                ORDER BY r.recipient_phone, r.created_at DESC
            ) sub
            ORDER BY lastSentAt DESC
            LIMIT :limit
            """)
    List<RecentRecipientProjection> findRecentRecipients(UUID senderId, int limit);
}
