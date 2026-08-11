-- =========================================================
-- Book Illustration Studio — PostgreSQL schema
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- cho gen_random_uuid()

-- ---------------------------------------------------------
-- users
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email      TEXT NOT NULL UNIQUE,
    name       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ---------------------------------------------------------
-- enums
-- ---------------------------------------------------------
DO $$ BEGIN
    CREATE TYPE project_status AS ENUM (
        'CREATED',
        'STYLE_SET',
        'CHARACTERS_GENERATED',
        'PORTRAITS_GENERATED',
        'CHAPTERS_GENERATED',
        'DONE'
    );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE step_state AS ENUM ('IDLE', 'RUNNING', 'FAILED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ---------------------------------------------------------
-- projects
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    title                     TEXT NOT NULL,
    book_text_file_path       TEXT NOT NULL,   -- path file .txt gốc trên filesystem

    status                    project_status NOT NULL DEFAULT 'CREATED',
    step_state                step_state NOT NULL DEFAULT 'IDLE',

    step_started_at           TIMESTAMPTZ,     -- null khi IDLE, dùng để detect stale/stuck
    step_failure_reason       TEXT,            -- null trừ khi step_state = FAILED

    style                     TEXT,            -- null cho tới khi bước Style xong

    -- Gemini interaction chaining — 2 chuỗi độc lập (text vs image), xem DECISIONS.md
    book_file_uri             TEXT,
    last_text_interaction_id  TEXT,
    last_image_interaction_id TEXT,

    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),

    version                   BIGINT NOT NULL DEFAULT 0   -- optimistic locking (JPA @Version)
);

CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects(user_id);

-- ---------------------------------------------------------
-- characters  (max 2 per project — enforce ở service layer)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS characters (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    position       INTEGER NOT NULL,
    name           TEXT NOT NULL,
    prompt         TEXT NOT NULL,
    portrait_ready BOOLEAN NOT NULL DEFAULT FALSE,
    portrait_path  TEXT,                       -- path ảnh trên filesystem, null cho tới khi ready

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_characters_project_position UNIQUE (project_id, position)
);

CREATE INDEX IF NOT EXISTS idx_characters_project_id ON characters(project_id);

-- ---------------------------------------------------------
-- chapters  (max 1 per project — enforce ở service layer)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS chapters (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    position            INTEGER NOT NULL,
    name                TEXT NOT NULL,
    prompt              TEXT NOT NULL,
    illustration_ready  BOOLEAN NOT NULL DEFAULT FALSE,
    illustration_path   TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_chapters_project_position UNIQUE (project_id, position)
);

CREATE INDEX IF NOT EXISTS idx_chapters_project_id ON chapters(project_id);

-- ---------------------------------------------------------
-- auto-update updated_at trên mỗi lần UPDATE projects
-- ---------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_projects_updated_at ON projects;
CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
