CREATE TABLE users (
    id         UUID         PRIMARY KEY,
    email      VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE social_identities (
    id             UUID         PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider       VARCHAR(32)  NOT NULL,
    subject        VARCHAR(255) NOT NULL,
    email          VARCHAR(320) NOT NULL,
    email_verified BOOLEAN      NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_social_identity UNIQUE (provider, subject)
);

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT uk_refresh_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_hash ON refresh_tokens(token_hash) WHERE revoked_at IS NULL;

TRUNCATE TABLE wallets CASCADE;
ALTER TABLE wallets DROP COLUMN user_id;
ALTER TABLE wallets ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
CREATE UNIQUE INDEX idx_wallet_user ON wallets(user_id);

TRUNCATE TABLE remittances CASCADE;
ALTER TABLE remittances DROP COLUMN sender_id;
ALTER TABLE remittances ADD COLUMN sender_id UUID NOT NULL REFERENCES users(id);
CREATE INDEX idx_remittance_sender ON remittances(sender_id);
