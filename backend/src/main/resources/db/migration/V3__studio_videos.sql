CREATE TABLE studio_videos (
    id VARCHAR(255) PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    total_bytes BIGINT NOT NULL,
    received_bytes BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    metadata TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    duration DOUBLE PRECISION NOT NULL,
    failure VARCHAR(500)
);
CREATE INDEX ix_studio_owner ON studio_videos(owner_id);
CREATE INDEX ix_studio_status ON studio_videos(status, created_at);
