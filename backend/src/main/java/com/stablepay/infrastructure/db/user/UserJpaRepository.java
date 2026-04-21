package com.stablepay.infrastructure.db.user;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
}
