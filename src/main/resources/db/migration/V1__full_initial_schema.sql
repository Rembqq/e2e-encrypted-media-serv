-- V1__full_initial_schema.sql
-- Полная начальная схема базы данных
-- Выполняется с нуля, все таблицы создаются заново
-- Используем IF NOT EXISTS для безопасности повторного запуска

-- 1. Установка расширения для генерации UUID (если ещё не установлено)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. Таблица пользователей (самая базовая, на неё ссылаются все остальные)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

-- 3. Таблица блобов (с user_id сразу)
CREATE TABLE IF NOT EXISTS blobs (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    hash TEXT NOT NULL,
    storage_key TEXT NOT NULL,
    size BIGINT NOT NULL,
    refcount INTEGER NOT NULL DEFAULT 1,
    metadata JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_blob_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Уникальный индекс по паре (user_id, hash) — per-user дедупликация
CREATE UNIQUE INDEX IF NOT EXISTS idx_blobs_user_hash ON blobs (user_id, hash);

-- Обычный индекс по user_id для фильтрации
CREATE INDEX IF NOT EXISTS idx_blobs_user_id ON blobs (user_id);

-- 4. Таблица снапшотов
CREATE TABLE IF NOT EXISTS snapshots (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id BIGINT NOT NULL,
                                         name VARCHAR(512) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    total_size BIGINT,
    file_count INTEGER,

    CONSTRAINT fk_snapshot_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_snapshots_user_id ON snapshots (user_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_created_at ON snapshots (created_at DESC);

-- 5. Таблица файлов в бэкапах
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