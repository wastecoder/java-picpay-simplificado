CREATE TABLE transactions (
    id              bigserial       PRIMARY KEY,
    from_user_id    bigint          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id  bigint          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value           numeric(19,2)   NOT NULL,
    description     text            NOT NULL,
    created_at      timestamp       NOT NULL,
    updated_at      timestamp       NOT NULL
);
