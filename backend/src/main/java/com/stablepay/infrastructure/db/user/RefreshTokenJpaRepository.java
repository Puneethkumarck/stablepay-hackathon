package com.stablepay.infrastructure.db.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = CURRENT_TIMESTAMP WHERE r.userId = :userId AND r.revokedAt IS NULL")
    void revokeByUserId(@Param("userId") UUID userId);
}
