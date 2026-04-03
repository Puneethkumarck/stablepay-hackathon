package com.stablepay.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.stablepay.domain.model.RemittanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "remittances")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RemittanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "remittance_id", nullable = false, unique = true)
    private UUID remittanceId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "amount_usdc", nullable = false, precision = 19, scale = 6)
    private BigDecimal amountUsdc;

    @Column(name = "amount_inr", precision = 19, scale = 2)
    private BigDecimal amountInr;

    @Column(name = "fx_rate", precision = 19, scale = 6)
    private BigDecimal fxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RemittanceStatus status;

    @Column(name = "escrow_pda")
    private String escrowPda;

    @Column(name = "claim_token_id")
    private String claimTokenId;

    @Column(name = "sms_notification_failed", nullable = false)
    private boolean smsNotificationFailed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
