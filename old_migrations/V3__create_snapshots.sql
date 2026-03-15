CREATE TABLE IF NOT EXISTS snapshots (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id BIGINT NOT NULL,
                                         name VARCHAR(512) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    total_size BIGINT,
    file_count INTEGER
    );

CREATE INDEX IF NOT EXISTS idx_snapshots_user_id ON snapshots (user_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_created_at ON snapshots (created_at DESC);