CREATE TABLE settlement_transaction (
    payment_id    VARCHAR(36)    PRIMARY KEY,
    circle_tx_id  VARCHAR(64),
    tx_hash       VARCHAR(66),
    state         VARCHAR(20)    NOT NULL,
    network_fee   NUMERIC(18,6),
    error_reason  VARCHAR(255),
    created_at    TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_settlement_transaction_circle_tx_id
    ON settlement_transaction (circle_tx_id);
