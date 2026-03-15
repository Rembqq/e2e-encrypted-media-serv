-- V4__create_backup_files.sql

CREATE TABLE IF NOT EXISTS backup_files (
                                            id BIGSERIAL PRIMARY KEY,
                                            snapshot_id BIGINT NOT NULL,
                                            user_id BIGINT NOT NULL,
                                            path TEXT NOT NULL,
                                            blob_id UUID NOT NULL,
                                            size BIGINT,
                                            modified_at TIMESTAMPTZ,

                                            CONSTRAINT fk_backup_file_snapshot
                                            FOREIGN KEY (snapshot_id) REFERENCES snapshots(id) ON DELETE CASCADE,

    CONSTRAINT fk_backup_file_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_backup_file_blob
    FOREIGN KEY (blob_id) REFERENCES blobs(id) ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_backup_files_snapshot_id ON backup_files (snapshot_id);
CREATE INDEX IF NOT EXISTS idx_backup_files_blob_id ON backup_files (blob_id);
CREATE INDEX IF NOT EXISTS idx_backup_files_user_id ON backup_files (user_id);