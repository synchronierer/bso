-- These tables were previously supplied by schema.sql. They must be part of
-- Flyway as well because SQL initialization is intentionally disabled.
CREATE TABLE IF NOT EXISTS app_users (
 id BIGSERIAL PRIMARY KEY,
 username VARCHAR(64) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','TEACHER')),
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS login_attempts (
 id BIGSERIAL PRIMARY KEY,
 username VARCHAR(128),
 client_ip VARCHAR(80),
 successful BOOLEAN NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_login_attempts_created_at ON login_attempts(created_at);
CREATE TABLE IF NOT EXISTS user_audit (
 id BIGSERIAL PRIMARY KEY,
 actor VARCHAR(64) NOT NULL,
 action VARCHAR(60) NOT NULL,
 target VARCHAR(128) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS roster_drafts (
 id BIGSERIAL PRIMARY KEY,
 username VARCHAR(64) NOT NULL,
 event_id BIGINT NOT NULL REFERENCES events(id),
 class_name VARCHAR(50) NOT NULL,
 entries JSONB NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(username,event_id,class_name)
);
