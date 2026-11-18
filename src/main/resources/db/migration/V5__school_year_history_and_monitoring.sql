CREATE TABLE IF NOT EXISTS enrollment_episodes (
 id BIGSERIAL PRIMARY KEY,
 student_pk BIGINT NOT NULL REFERENCES students(id),
 starts_on DATE NOT NULL,
 ends_on DATE,
 entry_reason VARCHAR(40) NOT NULL,
 exit_reason VARCHAR(40),
 created_by VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (ends_on IS NULL OR ends_on >= starts_on)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_open_enrollment_episode ON enrollment_episodes(student_pk) WHERE ends_on IS NULL;

CREATE TABLE IF NOT EXISTS student_assignments (
 id BIGSERIAL PRIMARY KEY,
 episode_id BIGINT NOT NULL REFERENCES enrollment_episodes(id),
 class_name VARCHAR(50) NOT NULL,
 grade INTEGER NOT NULL CHECK (grade BETWEEN 7 AND 13),
 track VARCHAR(30) NOT NULL,
 effective_from DATE NOT NULL,
 effective_until DATE,
 created_by VARCHAR(64) NOT NULL,
 CHECK (effective_until IS NULL OR effective_until >= effective_from)
);
CREATE INDEX IF NOT EXISTS idx_student_assignments_history ON student_assignments(episode_id,effective_from);

CREATE TABLE IF NOT EXISTS school_year_changes (
 id BIGSERIAL PRIMARY KEY,
 from_year_id BIGINT REFERENCES school_years(id),
 to_year_id BIGINT NOT NULL REFERENCES school_years(id),
 status VARCHAR(20) NOT NULL CHECK (status IN ('VORSCHAU','ANGEWENDET','ABGEBROCHEN')),
 created_by VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 applied_at TIMESTAMPTZ
);
CREATE TABLE IF NOT EXISTS school_year_change_decisions (
 id BIGSERIAL PRIMARY KEY,
 change_id BIGINT NOT NULL REFERENCES school_year_changes(id),
 student_pk BIGINT NOT NULL REFERENCES students(id),
 decision VARCHAR(20) NOT NULL CHECK (decision IN ('VERSETZT','WIEDERHOLUNG','AUSTRITT','WIEDEREINTRITT','UNVERAENDERT')),
 class_name VARCHAR(50), grade INTEGER CHECK (grade BETWEEN 7 AND 13), track VARCHAR(30),
 notes VARCHAR(500),
 UNIQUE(change_id,student_pk)
);

ALTER TABLE events ADD COLUMN IF NOT EXISTS partner VARCHAR(200);
ALTER TABLE events ADD COLUMN IF NOT EXISTS required_for_grade INTEGER CHECK (required_for_grade IS NULL OR required_for_grade BETWEEN 7 AND 13);
ALTER TABLE events ADD COLUMN IF NOT EXISTS required_for_track VARCHAR(30);
CREATE TABLE IF NOT EXISTS event_requirements (
 id BIGSERIAL PRIMARY KEY,
 school_year_id BIGINT NOT NULL REFERENCES school_years(id),
 grade INTEGER NOT NULL CHECK (grade BETWEEN 7 AND 13),
 track VARCHAR(30),
 kind VARCHAR(30) NOT NULL DEFAULT 'WAHLPFLICHT',
 title VARCHAR(200) NOT NULL,
 equivalence_group_id BIGINT REFERENCES equivalence_groups(id),
 minimum_count INTEGER NOT NULL DEFAULT 1 CHECK (minimum_count > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_event_requirements_key ON event_requirements(school_year_id,grade,COALESCE(track,''),title);

-- Existing active students receive an initial episode without changing their data.
INSERT INTO enrollment_episodes(student_pk,starts_on,entry_reason,created_by)
SELECT id, DATE '2026-08-01', 'BESTAND', 'migration'
FROM students s
WHERE active AND NOT EXISTS (SELECT 1 FROM enrollment_episodes e WHERE e.student_pk=s.id);
INSERT INTO student_assignments(episode_id,class_name,grade,track,effective_from,created_by)
SELECT e.id,s.class_name,s.grade,s.track,e.starts_on,'migration'
FROM enrollment_episodes e JOIN students s ON s.id=e.student_pk
WHERE NOT EXISTS (SELECT 1 FROM student_assignments a WHERE a.episode_id=e.id);
