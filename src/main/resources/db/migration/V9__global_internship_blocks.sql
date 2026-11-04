-- Blocks are school-wide; class is only a filter when choosing pupils.
-- Preserve historical class_name values for audit, but they no longer constrain membership.
ALTER TABLE internship_blocks ALTER COLUMN class_name DROP NOT NULL;
COMMENT ON COLUMN internship_blocks.class_name IS 'Legacy value for older blocks; not a class assignment. New blocks use NULL.';
