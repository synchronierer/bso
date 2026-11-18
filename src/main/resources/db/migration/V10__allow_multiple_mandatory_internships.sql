-- New school decision: multiple compulsory internships are permitted,
-- including in the same school year and within the same coin block.
-- Preserve existing rows and the once-per-school-year limit for ZUSATZ.
DROP INDEX IF EXISTS uq_mandatory_internship_block;
DROP INDEX IF EXISTS uq_mandatory_internship_episode_block;
