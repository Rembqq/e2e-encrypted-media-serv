CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS blobs (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    hash TEXT NOT NULL,
    storage_key TEXT NOT NULL,
    size BIGINT NOT NULL,
    refcount INTEGER NOT NULL DEFAULT 1,
    metadata JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- Уникальный индекс по паре (user_id, hash) — для per-user дедупликации
CREATE UNIQUE INDEX IF NOT EXISTS idx_blobs_user_hash ON blobs (user_id, hash);

-- Обычный индекс по user_id для фильтрации
CREATE INDEX IF NOT EXISTS idx_blobs_user_id ON blobs (user_id);