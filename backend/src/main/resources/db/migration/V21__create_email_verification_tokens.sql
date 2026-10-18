CREATE TABLE credentials.email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL
        REFERENCES credentials.users(id)
        ON DELETE CASCADE,

    token_hash CHAR(64) NOT NULL UNIQUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,

    CONSTRAINT email_verification_token_hash_format
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT email_verification_expiry_valid
        CHECK (expires_at > created_at),

    CONSTRAINT email_verification_used_valid
        CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX idx_email_verification_user_created
    ON credentials.email_verification_tokens(user_id, created_at DESC);

CREATE INDEX idx_email_verification_expiry
    ON credentials.email_verification_tokens(expires_at);

CREATE INDEX idx_email_verification_unused
    ON credentials.email_verification_tokens(token_hash)
    WHERE used_at IS NULL;