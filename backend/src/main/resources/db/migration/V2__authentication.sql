CREATE TABLE auth_accounts (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    birth_date DATE,
    terms_accepted_at TIMESTAMPTZ NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    session_version INT NOT NULL DEFAULT 0
);
CREATE TABLE auth_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_auth_tokens_user ON auth_tokens(user_id);
