CREATE TABLE remittance_status_events (
    id              BIGSERIAL       PRIMARY KEY,
    remittance_id   UUID            NOT NULL REFERENCES remittances(remittance_id),
    status          VARCHAR(30)     NOT NULL,
    message         VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_remittance_status_events_remittance_id
    ON remittance_status_events(remittance_id, created_at, id);
