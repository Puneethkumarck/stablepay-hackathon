CREATE TABLE funding_orders (
    id                         BIGSERIAL PRIMARY KEY,
    funding_id                 UUID UNIQUE NOT NULL,
    wallet_id                  BIGINT NOT NULL REFERENCES wallets(id),
    amount_usdc                NUMERIC(19, 6) NOT NULL,
    stripe_payment_intent_id   VARCHAR(255) UNIQUE,
    status                     VARCHAR(50) NOT NULL DEFAULT 'PAYMENT_CONFIRMED',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ
);

CREATE INDEX idx_funding_orders_wallet_id ON funding_orders(wallet_id);
CREATE INDEX idx_funding_orders_stripe_pi ON funding_orders(stripe_payment_intent_id);
CREATE INDEX idx_funding_orders_status ON funding_orders(status);

CREATE UNIQUE INDEX idx_funding_orders_one_active_per_wallet
    ON funding_orders(wallet_id)
    WHERE status = 'PAYMENT_CONFIRMED';
