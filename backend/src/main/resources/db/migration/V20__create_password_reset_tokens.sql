-- Password-reset tokens are stored as SHA-256 hashes, never as raw reset tokens.
-- The backend must generate a cryptographically secure random token, email the raw
-- token to the user, and persist only its hash here.

CREATE TABLE credentials.password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    token_hash CHAR(64) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,

    CONSTRAINT password_reset_token_user_fk
        FOREIGN KEY (user_id)
        REFERENCES credentials.users (id)
        ON DELETE CASCADE,

    CONSTRAINT password_reset_token_hash_unique
        UNIQUE (token_hash),

    CONSTRAINT password_reset_token_hash_format
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT password_reset_token_expiry_check
        CHECK (expires_at > created_at),

    CONSTRAINT password_reset_token_used_check
        CHECK (used_at IS NULL OR used_at >= created_at)
);

-- Supports finding outstanding tokens for an account and invalidating them
-- when a new reset is requested or a password is successfully changed.
CREATE INDEX password_reset_tokens_user_created_idx
    ON credentials.password_reset_tokens (user_id, created_at DESC);

-- Supports scheduled cleanup of expired tokens.
CREATE INDEX password_reset_tokens_expiry_idx
    ON credentials.password_reset_tokens (expires_at);

-- Only unused tokens need to be considered during token verification.
CREATE INDEX password_reset_tokens_unused_hash_idx
    ON credentials.password_reset_tokens (token_hash)
    WHERE used_at IS NULL;
