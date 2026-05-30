CREATE TABLE payment (
    payment_id            VARCHAR(36)   PRIMARY KEY,
    agent_id              VARCHAR(36)   NOT NULL,
    owner_id              VARCHAR(36)   NOT NULL,
    idempotency_key       VARCHAR(255)  NOT NULL,
    request_fingerprint   VARCHAR(66)   NOT NULL,
    recipient_address     VARCHAR(42)   NOT NULL,
    amount                NUMERIC(18,6) NOT NULL,
    currency              VARCHAR(10)   NOT NULL,
    memo                  VARCHAR(256),
    status                VARCHAR(20)   NOT NULL,
    rejection_reason      VARCHAR(30),
    failure_reason        VARCHAR(30),
    tx_hash               VARCHAR(66),
    on_chain_ref          VARCHAR(66),
    policy_evaluation_id  VARCHAR(36),
    compliance_risk_score INT,
    metadata              JSONB,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,
    completed_at          TIMESTAMPTZ,
    CONSTRAINT uq_payment_idem UNIQUE (agent_id, idempotency_key)
);

CREATE INDEX idx_payment_agent_status ON payment (agent_id, status);
