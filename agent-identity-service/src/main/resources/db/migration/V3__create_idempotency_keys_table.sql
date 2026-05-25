CREATE TABLE idempotency_keys (
    idempotency_key UUID            NOT NULL,
    owner_id        UUID            NOT NULL,
    endpoint        VARCHAR(255)    NOT NULL,
    response_status INTEGER         NOT NULL,
    response_body   TEXT            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ     NOT NULL DEFAULT (now() + INTERVAL '24 hours'),
    CONSTRAINT idempotency_keys_pkey PRIMARY KEY (idempotency_key, owner_id)
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);
