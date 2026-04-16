package com.stablepay.infrastructure.db.wallet;

import static com.stablepay.testutil.WalletFixtures.SOME_BALANCE;
import static com.stablepay.testutil.WalletFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.WalletFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PEER_KEY_SHARE_DATA;
import static com.stablepay.testutil.WalletFixtures.SOME_PUBLIC_KEY;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.SOME_UPDATED_AT;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.SOME_WALLET_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.wallet.model.Wallet;

class WalletEntityMapperTest {

    private final WalletEntityMapper mapper = new WalletEntityMapperImpl();

    @Test
    void shouldMapDomainToEntityAndBack() {
        // given
        var domain = Wallet.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(SOME_BALANCE)
                .totalBalance(SOME_BALANCE)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        // when
        var entity = mapper.toEntity(domain);
        var backToDomain = mapper.toDomain(entity);

        // then
        assertThat(backToDomain)
                .usingRecursiveComparison()
                .isEqualTo(domain);
    }

    @Test
    void shouldMapEntityToDomainWithAllFields() {
        // given
        var entity = WalletEntity.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(SOME_BALANCE)
                .totalBalance(SOME_BALANCE)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        // when
        var domain = mapper.toDomain(entity);

        // then
        var expected = Wallet.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .publicKey(SOME_PUBLIC_KEY)
                .keyShareData(SOME_KEY_SHARE_DATA)
                .peerKeyShareData(SOME_PEER_KEY_SHARE_DATA)
                .availableBalance(SOME_BALANCE)
                .totalBalance(SOME_BALANCE)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        assertThat(domain)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
