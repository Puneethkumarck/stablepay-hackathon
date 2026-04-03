CREATE TABLE wallets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL UNIQUE,
    solana_address  VARCHAR(255) NOT NULL UNIQUE,
    available_balance NUMERIC(19, 6) NOT NULL DEFAULT 0,
    total_balance   NUMERIC(19, 6) NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE remittances (
    id                      BIGSERIAL PRIMARY KEY,
    remittance_id           UUID NOT NULL UNIQUE,
    sender_id               VARCHAR(255) NOT NULL,
    recipient_phone         VARCHAR(50) NOT NULL,
    amount_usdc             NUMERIC(19, 6) NOT NULL,
    amount_inr              NUMERIC(19, 2),
    fx_rate                 NUMERIC(19, 6),
    status                  VARCHAR(20) NOT NULL,
    escrow_pda              VARCHAR(255),
    claim_token_id          VARCHAR(255),
    sms_notification_failed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    expires_at              TIMESTAMPTZ
);

CREATE INDEX idx_remittances_sender_id ON remittances(sender_id);
CREATE INDEX idx_remittances_status ON remittances(status);

CREATE TABLE claim_tokens (
    id              BIGSERIAL PRIMARY KEY,
    remittance_id   UUID NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    claimed         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_claim_tokens_token ON claim_tokens(token);
CREATE INDEX idx_claim_tokens_remittance_id ON claim_tokens(remittance_id);
