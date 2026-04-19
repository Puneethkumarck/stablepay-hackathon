ALTER TABLE remittances
    ADD COLUMN payout_id VARCHAR(255),
    ADD COLUMN payout_provider_status VARCHAR(50),
    ADD COLUMN payout_failure_reason VARCHAR(500);

CREATE UNIQUE INDEX idx_remittances_payout_id
    ON remittances(payout_id)
    WHERE payout_id IS NOT NULL;
