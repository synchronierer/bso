CREATE TABLE IF NOT EXISTS internships (
 id BIGSERIAL PRIMARY KEY,
 student_pk BIGINT NOT NULL REFERENCES students(id),
 school_year_id BIGINT REFERENCES school_years(id),
 kind VARCHAR(20) NOT NULL CHECK (kind IN ('PFLICHT','ZUSATZ')),
 company VARCHAR(200) NOT NULL,
 starts_on DATE NOT NULL,
 ends_on DATE NOT NULL,
 recognized_days INTEGER NOT NULL CHECK (recognized_days BETWEEN 1 AND 10),
 coins NUMERIC(8,1) NOT NULL CHECK (coins >= 10 AND coins <= 100 AND MOD(coins,10)=0),
 finalised BOOLEAN NOT NULL DEFAULT FALSE,
 recorded_by VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (ends_on >= starts_on),
 CHECK ((kind='PFLICHT' AND coins=recognized_days*10) OR (kind='ZUSATZ' AND recognized_days <= 5 AND coins=recognized_days*10))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mandatory_internship_block
  ON internships(student_pk,kind,school_year_id)
  WHERE kind='PFLICHT' AND finalised;
CREATE UNIQUE INDEX IF NOT EXISTS uq_extra_internship_year
  ON internships(student_pk,school_year_id)
  WHERE kind='ZUSATZ' AND finalised;

CREATE TABLE IF NOT EXISTS certificates (
 id BIGSERIAL PRIMARY KEY,
 student_pk BIGINT NOT NULL REFERENCES students(id),
 status VARCHAR(30) NOT NULL DEFAULT 'AKTUELL',
 issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 issued_by VARCHAR(64) NOT NULL,
 UNIQUE(id,student_pk)
);
CREATE TABLE IF NOT EXISTS certificate_versions (
 id BIGSERIAL PRIMARY KEY,
 certificate_id BIGINT NOT NULL REFERENCES certificates(id),
 version INTEGER NOT NULL,
 as_of DATE NOT NULL,
 level VARCHAR(30) NOT NULL,
 snapshot JSONB NOT NULL,
 pdf_data BYTEA NOT NULL,
 docx_data BYTEA NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(certificate_id,version)
);
CREATE INDEX IF NOT EXISTS idx_certificate_student ON certificates(student_pk);

CREATE TABLE IF NOT EXISTS import_runs (
 id BIGSERIAL PRIMARY KEY,
 kind VARCHAR(30) NOT NULL,
 content_hash VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL,
 row_count INTEGER NOT NULL DEFAULT 0,
 created_by VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(kind,content_hash)
);
