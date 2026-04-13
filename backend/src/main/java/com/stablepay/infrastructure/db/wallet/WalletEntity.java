package com.stablepay.infrastructure.db.wallet;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "solana_address", nullable = false, unique = true)
    private String solanaAddress;

    @Column(name = "public_key")
    private byte[] publicKey;

    @Column(name = "key_share_data")
    private byte[] keyShareData;

    @Column(name = "peer_key_share_data")
    private byte[] peerKeyShareData;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 6)
    private BigDecimal availableBalance;

    @Column(name = "total_balance", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
