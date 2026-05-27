CREATE TABLE agents (
    agent_id        UUID            NOT NULL,
    owner_id        UUID            NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    purpose         VARCHAR(256)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PROVISIONING',
    wallet_id       VARCHAR(255),
    wallet_address  VARCHAR(42),
    on_chain_tx_hash VARCHAR(66),
    policy_hash     VARCHAR(66),
    metadata_hash   VARCHAR(66)     NOT NULL,
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT agents_pkey PRIMARY KEY (agent_id),
    CONSTRAINT agents_owner_fk FOREIGN KEY (owner_id) REFERENCES owners (owner_id)
);

CREATE UNIQUE INDEX idx_agents_owner_name ON agents (owner_id, LOWER(name));
CREATE INDEX idx_agents_owner_id ON agents (owner_id);
CREATE INDEX idx_agents_status ON agents (status);
