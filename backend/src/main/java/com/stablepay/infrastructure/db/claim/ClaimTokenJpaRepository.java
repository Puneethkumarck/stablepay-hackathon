package com.stablepay.infrastructure.db.claim;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClaimTokenJpaRepository extends JpaRepository<ClaimTokenEntity, Long> {
    Optional<ClaimTokenEntity> findByToken(String token);
}
