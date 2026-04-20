package com.stablepay.infrastructure.db.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stablepay.domain.auth.model.AppUser;
import com.stablepay.domain.auth.model.RefreshToken;
import com.stablepay.domain.auth.model.SocialIdentity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    AppUser toDomain(UserEntity entity);

    @Mapping(target = "updatedAt", ignore = true)
    UserEntity toEntity(AppUser domain);

    SocialIdentity toDomain(SocialIdentityEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SocialIdentityEntity toEntity(SocialIdentity domain);

    RefreshToken toDomain(RefreshTokenEntity entity);

    @Mapping(target = "issuedAt", ignore = true)
    RefreshTokenEntity toEntity(RefreshToken domain);
}
