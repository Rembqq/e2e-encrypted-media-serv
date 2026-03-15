-- V5__add_foreign_keys_and_constraints.sql

-- Добавляем недостающие FK на blobs (если их не было раньше)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_blob_user'
    ) THEN
ALTER TABLE blobs
    ADD CONSTRAINT fk_blob_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
END IF;
END $$;

-- Делаем user_id NOT NULL везде (если ещё не сделано)
DO $$
BEGIN
ALTER TABLE blobs        ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE snapshots    ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE backup_files ALTER COLUMN user_id SET NOT NULL;
EXCEPTION WHEN OTHERS THEN
    -- если уже NOT NULL — просто пропускаем
END $$;

ALTER TABLE blobs        ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE snapshots    ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE backup_files ALTER COLUMN user_id SET NOT NULL;