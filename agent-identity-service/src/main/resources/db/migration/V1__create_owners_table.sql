CREATE TABLE owners (
    owner_id        UUID            NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    wallet_address  VARCHAR(42)     NOT NULL,
    api_key_hash    VARCHAR(64)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT owners_pkey PRIMARY KEY (owner_id)
);

CREATE UNIQUE INDEX idx_owners_email ON owners (LOWER(email));
CREATE UNIQUE INDEX idx_owners_wallet ON owners (LOWER(wallet_address));
CREATE INDEX idx_owners_api_key_hash ON owners (api_key_hash);
