CREATE TABLE hold_review (
    review_id          UUID PRIMARY KEY,
    screening_id       UUID         NOT NULL REFERENCES screening_result(screening_id),
    payment_id         UUID         NOT NULL UNIQUE,
    agent_id           UUID         NOT NULL,
    state              VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    reviewer_principal VARCHAR(255),
    reviewer_role      VARCHAR(32),
    reason             TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at         TIMESTAMPTZ
);

CREATE INDEX idx_hold_review_queue ON hold_review (state, created_at);
