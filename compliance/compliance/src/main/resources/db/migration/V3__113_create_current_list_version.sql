CREATE TABLE current_list_version (
    id          SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    version_id  UUID NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
