CREATE TABLE gas_usage (
    id              UUID            NOT NULL,
    owner_id        UUID            NOT NULL,
    agent_id        UUID,
    operation       VARCHAR(50)     NOT NULL,
    tx_hash         VARCHAR(66)     NOT NULL,
    gas_used        BIGINT          NOT NULL,
    gas_cost_usdc   NUMERIC(18, 8)  NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT gas_usage_pkey PRIMARY KEY (id),
    CONSTRAINT gas_usage_owner_fk FOREIGN KEY (owner_id) REFERENCES owners (owner_id)
);

CREATE INDEX idx_gas_usage_owner ON gas_usage (owner_id, created_at);
