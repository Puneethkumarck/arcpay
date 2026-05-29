CREATE TABLE screening_check (
    id           UUID PRIMARY KEY,
    screening_id UUID        NOT NULL REFERENCES screening_result(screening_id),
    type         VARCHAR(32) NOT NULL,
    result       VARCHAR(16) NOT NULL,
    match_score  INTEGER     NOT NULL,
    details      JSONB
);
