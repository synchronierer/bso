-- Internship planning is separate from final coin crediting.
CREATE TABLE internship_blocks (
 id BIGSERIAL PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 class_name VARCHAR(50) NOT NULL,
 kind VARCHAR(20) NOT NULL CHECK (kind IN ('PFLICHT','ZUSATZ')),
 starts_on DATE NOT NULL,
 ends_on DATE NOT NULL,
 created_by VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (ends_on >= starts_on)
);
CREATE TABLE internship_placements (
 id BIGSERIAL PRIMARY KEY,
 block_id BIGINT NOT NULL REFERENCES internship_blocks(id),
 student_pk BIGINT NOT NULL REFERENCES students(id),
 episode_id BIGINT NOT NULL REFERENCES enrollment_episodes(id),
 company VARCHAR(200) NOT NULL,
 recognized_days INTEGER,
 internship_id BIGINT UNIQUE REFERENCES internships(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(block_id,student_pk),
 CHECK (length(trim(company))>0),
 CHECK (recognized_days IS NULL OR recognized_days BETWEEN 1 AND 10)
);
CREATE INDEX idx_internship_placements_block ON internship_placements(block_id);
COMMENT ON TABLE internship_placements IS 'Planning rows never award coins. A link to a finalised internship is added only after explicit confirmation.';
