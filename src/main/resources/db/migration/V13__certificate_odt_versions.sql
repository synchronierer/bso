-- Keep historical DOCX versions untouched and introduce ODT storage for new versions.
ALTER TABLE certificate_versions ADD COLUMN odt_data BYTEA;
ALTER TABLE certificate_versions ALTER COLUMN docx_data DROP NOT NULL;
COMMENT ON COLUMN certificate_versions.odt_data IS 'Private ODT-based certificate with embedded approved signature, rendered from external template.';
