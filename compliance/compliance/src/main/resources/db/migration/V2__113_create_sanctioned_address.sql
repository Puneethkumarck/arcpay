CREATE TABLE sanctioned_address (
    id          UUID PRIMARY KEY,
    version_id  UUID         NOT NULL REFERENCES sanctions_list_version(version_id),
    address     VARCHAR(64)  NOT NULL,
    source      VARCHAR(32)  NOT NULL,
    source_ref  VARCHAR(128)
);

CREATE INDEX idx_sanctioned_address_lookup ON sanctioned_address (version_id, address);
