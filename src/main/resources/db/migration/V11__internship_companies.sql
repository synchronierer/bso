-- Reusable company directory for internship placements.
-- Company names already stored on internships/placements stay unchanged.
CREATE TABLE internship_companies (
 id BIGSERIAL PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (length(trim(name)) > 0)
);
CREATE UNIQUE INDEX uq_internship_companies_name_normalized
 ON internship_companies (lower(btrim(name)));
