CREATE TABLE spending_ledger (
    entry_id        UUID            NOT NULL,
    agent_id        UUID            NOT NULL,
    payment_id      UUID            NOT NULL,
    amount          NUMERIC(18, 6)  NOT NULL,
    recipient       VARCHAR(42)     NOT NULL,
    executed_at     TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT spending_ledger_pkey PRIMARY KEY (entry_id),
    CONSTRAINT spending_ledger_payment_uq UNIQUE (payment_id)
);

CREATE INDEX idx_spending_agent_time ON spending_ledger (agent_id, executed_at);
