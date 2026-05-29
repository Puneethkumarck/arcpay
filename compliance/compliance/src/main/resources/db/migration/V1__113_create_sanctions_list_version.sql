CREATE TABLE sanctions_list_version (
    version_id          UUID PRIMARY KEY,
    source              VARCHAR(32)  NOT NULL,
    source_published_at TIMESTAMPTZ,
    downloaded_at       TIMESTAMPTZ  NOT NULL,
    record_count        INTEGER      NOT NULL,
    checksum            VARCHAR(128) NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
);
