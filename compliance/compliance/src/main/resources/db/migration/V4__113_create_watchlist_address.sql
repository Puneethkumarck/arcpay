CREATE TABLE watchlist_address (
    id       UUID PRIMARY KEY,
    address  VARCHAR(64) NOT NULL UNIQUE,
    label    VARCHAR(255),
    added_by VARCHAR(255) NOT NULL,
    added_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
