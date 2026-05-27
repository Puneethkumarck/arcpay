CREATE TABLE policies (
    policy_id       UUID            NOT NULL,
    agent_id        UUID            NOT NULL,
    owner_id        UUID            NOT NULL,
    version         INT             NOT NULL,
    rules           JSONB           NOT NULL,
    policy_hash     VARCHAR(66)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT policies_pkey PRIMARY KEY (policy_id),
    CONSTRAINT policies_agent_version_uq UNIQUE (agent_id, version)
);

CREATE INDEX idx_policies_agent_status ON policies (agent_id, status);
