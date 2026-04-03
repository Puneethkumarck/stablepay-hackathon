package com.stablepay.infrastructure.persistence;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

interface WalletJpaRepository extends JpaRepository<WalletEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "4000")})
    Optional<WalletEntity> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "4000")})
    Optional<WalletEntity> findById(Long id);
}
