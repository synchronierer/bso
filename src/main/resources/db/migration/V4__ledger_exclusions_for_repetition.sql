ALTER TABLE coin_grants ADD COLUMN IF NOT EXISTS student_grade INTEGER CHECK(student_grade BETWEEN 7 AND 13);
ALTER TABLE coin_grants ADD COLUMN IF NOT EXISTS excluded BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE coin_grants ADD COLUMN IF NOT EXISTS exclusion_reason VARCHAR(80);
CREATE INDEX IF NOT EXISTS idx_coin_grants_effective ON coin_grants(student_pk,excluded,recorded_at);
