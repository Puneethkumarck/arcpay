CREATE TABLE screening_result (
    screening_id      UUID PRIMARY KEY,
    payment_id        UUID         NOT NULL UNIQUE,
    agent_id          UUID         NOT NULL,
    recipient_address VARCHAR(64)  NOT NULL,
    verdict           VARCHAR(8)   NOT NULL,
    risk_score        INTEGER      NOT NULL,
    list_version_id   UUID,
    screened_at       TIMESTAMPTZ  NOT NULL,
    duration_ms       BIGINT       NOT NULL
);

CREATE INDEX idx_screening_result_agent ON screening_result (agent_id, screened_at);
