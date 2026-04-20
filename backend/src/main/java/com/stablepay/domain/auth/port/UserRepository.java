package com.stablepay.domain.auth.port;

import java.util.Optional;
import java.util.UUID;

import com.stablepay.domain.auth.model.AppUser;

public interface UserRepository {
    Optional<AppUser> findById(UUID id);
    AppUser save(AppUser user);
}
