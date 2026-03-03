CREATE TABLE snapshots (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(512) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    total_size BIGINT,
    file_count INTEGER
);

CREATE TABLE backup_files (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES snapshots(id) ON DELETE CASCADE,
    path TEXT NOT NULL,
    blob_id UUID NOT NULL REFERENCES blobs(id) ON DELETE RESTRICT,
    size BIGINT,
    modified_at TIMESTAMPTZ,

    CONSTRAINT fk_backup_file_blob
                          FOREIGN KEY (blob_id) REFERENCES blobs(id) ON DELETE RESTRICT
);

CREATE INDEX idx_backup_files_snapshots_id ON backup_files(snapshot_id);
CREATE INDEX idx_backup_files_blob_id ON backup_files(blob_id);