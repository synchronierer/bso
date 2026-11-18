-- Additive baseline. Existing demo tables are deliberately preserved.
CREATE TABLE IF NOT EXISTS students (
 id BIGSERIAL PRIMARY KEY,
 student_id VARCHAR(80) NOT NULL UNIQUE,
 first_name VARCHAR(150) NOT NULL,
 last_name VARCHAR(150) NOT NULL,
 class_name VARCHAR(50) NOT NULL,
 grade INTEGER NOT NULL CHECK (grade BETWEEN 7 AND 13),
 track VARCHAR(30) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS events (
 id BIGSERIAL PRIMARY KEY,
 title VARCHAR(255) NOT NULL,
 end_date DATE NOT NULL,
 coins NUMERIC(8,1) NOT NULL CHECK (coins >= 0.5 AND MOD(coins,0.5)=0),
 kind VARCHAR(30) NOT NULL DEFAULT 'WAHLPFLICHT',
 status VARCHAR(30) NOT NULL DEFAULT 'AKTIV',
 version BIGINT NOT NULL DEFAULT 0
);
ALTER TABLE students ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE students ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE events ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'AKTIV';
ALTER TABLE events ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS coin_grants (
 id BIGSERIAL PRIMARY KEY,
 student_pk BIGINT NOT NULL REFERENCES students(id),
 event_id BIGINT NOT NULL REFERENCES events(id),
 coins NUMERIC(8,1) NOT NULL CHECK (coins >= 0 AND MOD(coins,0.5)=0),
 recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 recorded_by VARCHAR(64),
 UNIQUE(student_pk,event_id)
);
ALTER TABLE coin_grants ADD COLUMN IF NOT EXISTS recorded_by VARCHAR(64);

CREATE TABLE IF NOT EXISTS school_years (
 id BIGSERIAL PRIMARY KEY,
 name VARCHAR(9) NOT NULL UNIQUE,
 starts_on DATE NOT NULL,
 ends_on DATE NOT NULL,
 CHECK (ends_on > starts_on)
);
CREATE TABLE IF NOT EXISTS equivalence_groups (
 id BIGSERIAL PRIMARY KEY,
 name VARCHAR(150) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS event_equivalences (
 event_id BIGINT PRIMARY KEY REFERENCES events(id),
 group_id BIGINT NOT NULL REFERENCES equivalence_groups(id)
);
CREATE TABLE IF NOT EXISTS threshold_rules (
 id BIGSERIAL PRIMARY KEY,
 grade INTEGER NOT NULL CHECK(grade BETWEEN 8 AND 13),
 track VARCHAR(30) NOT NULL,
 success NUMERIC(8,1), special_success NUMERIC(8,1), distinction NUMERIC(8,1),
 valid_from DATE NOT NULL,
 valid_until DATE,
 changed_by VARCHAR(64),
 CHECK (valid_until IS NULL OR valid_until > valid_from),
 UNIQUE(grade,track,valid_from)
);
CREATE TABLE IF NOT EXISTS repeat_decisions (
 id BIGSERIAL PRIMARY KEY,
 student_pk BIGINT NOT NULL REFERENCES students(id),
 repeated_grade INTEGER NOT NULL CHECK(repeated_grade BETWEEN 7 AND 13),
 effective_on DATE NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 decided_by VARCHAR(64) NOT NULL,
 decided_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS audit_events (
 id BIGSERIAL PRIMARY KEY,
 actor VARCHAR(64) NOT NULL,
 action VARCHAR(80) NOT NULL,
 object_type VARCHAR(80) NOT NULL,
 object_id VARCHAR(128) NOT NULL,
 before_json JSONB,
 after_json JSONB,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX IF NOT EXISTS idx_coin_grants_student ON coin_grants(student_pk);
CREATE INDEX IF NOT EXISTS idx_events_end_date ON events(end_date);

-- Safe defaults for the first configured school year. Operators may add future versions.
INSERT INTO school_years(name,starts_on,ends_on)
VALUES ('2026/27', DATE '2026-08-01', DATE '2027-07-31')
ON CONFLICT (name) DO NOTHING;

INSERT INTO threshold_rules(grade,track,success,special_success,distinction,valid_from)
VALUES
 (8,'BR',11,18,22,DATE '2026-08-01'),
 (8,'V10/S1/Ü11',10,17,21,DATE '2026-08-01'),
 (9,'BR',128,151,171,DATE '2026-08-01'),
 (9,'V10/S1/Ü11',24,47,67,DATE '2026-08-01'),
 (10,'S1/Ü11',40,70,97,DATE '2026-08-01'),
 (11,'MSS',116,137,153,DATE '2026-08-01'),
 (12,'MSS',132,160,183,DATE '2026-08-01')
ON CONFLICT (grade,track,valid_from) DO NOTHING;
