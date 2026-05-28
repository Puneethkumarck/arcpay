CREATE TABLE spending_locks (
    agent_id        UUID            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT spending_locks_pkey PRIMARY KEY (agent_id)
);
