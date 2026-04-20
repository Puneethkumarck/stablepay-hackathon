package com.stablepay.infrastructure.db.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SocialIdentityJpaRepository extends JpaRepository<SocialIdentityEntity, UUID> {
    Optional<SocialIdentityEntity> findByProviderAndSubject(String provider, String subject);
}
