CREATE TABLE policyengine_outbox_record (
    id              VARCHAR(255)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    record_key      VARCHAR(255)    NOT NULL,
    record_type     VARCHAR(255)    NOT NULL,
    payload         TEXT            NOT NULL,
    context         TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ,
    failure_count   INTEGER         NOT NULL DEFAULT 0,
    failure_reason  TEXT,
    partition_no    INTEGER         NOT NULL,
    handler_id      VARCHAR(255),
    CONSTRAINT policyengine_outbox_record_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_policyengine_outbox_record_status
    ON policyengine_outbox_record (status, next_retry_at);

CREATE INDEX idx_policyengine_outbox_record_key
    ON policyengine_outbox_record (record_key, created_at);

CREATE INDEX idx_policyengine_outbox_record_partition
    ON policyengine_outbox_record (status, partition_no, next_retry_at);

CREATE TABLE policyengine_outbox_instance (
    instance_id     VARCHAR(255)    NOT NULL,
    hostname        VARCHAR(255),
    port            INTEGER,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    started_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    last_heartbeat  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT policyengine_outbox_instance_pkey PRIMARY KEY (instance_id)
);

CREATE TABLE policyengine_outbox_partition (
    partition_number INTEGER         NOT NULL,
    instance_id      VARCHAR(255),
    version          BIGINT          NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT policyengine_outbox_partition_pkey PRIMARY KEY (partition_number)
);
