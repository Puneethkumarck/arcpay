CREATE TABLE policy_evaluations (
    evaluation_id       UUID            NOT NULL,
    agent_id            UUID            NOT NULL,
    policy_id           UUID            NOT NULL,
    verdict             VARCHAR(20)     NOT NULL,
    rule_results        JSONB           NOT NULL,
    requested_amount    NUMERIC(18, 6)  NOT NULL,
    recipient_address   VARCHAR(42)     NOT NULL,
    duration_ms         INT             NOT NULL,
    dry_run             BOOLEAN         NOT NULL DEFAULT false,
    evaluated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT policy_evaluations_pkey PRIMARY KEY (evaluation_id)
);

CREATE INDEX idx_evaluations_agent ON policy_evaluations (agent_id, evaluated_at);
