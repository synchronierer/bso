-- Keep long registered name and add the school's familiar short label and sorting.
ALTER TABLE internship_companies
 ADD COLUMN short_name VARCHAR(200),
 ADD COLUMN sort_order INTEGER;
UPDATE internship_companies SET short_name=name WHERE short_name IS NULL;
ALTER TABLE internship_companies ALTER COLUMN short_name SET NOT NULL;
CREATE INDEX idx_internship_companies_sort ON internship_companies(sort_order,short_name);
