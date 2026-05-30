CREATE TABLE spending_reservation (
    payment_id      UUID            NOT NULL,
    agent_id        UUID            NOT NULL,
    amount          NUMERIC(18, 6)  NOT NULL,
    recipient       VARCHAR(42)     NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT spending_reservation_pkey PRIMARY KEY (payment_id)
);

CREATE INDEX idx_reservation_agent_status ON spending_reservation (agent_id, status);
