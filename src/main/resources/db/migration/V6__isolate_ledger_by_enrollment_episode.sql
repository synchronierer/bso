-- Every earning belongs to exactly one school-attendance episode. Existing
-- rows are assigned by their fachliches date; no historical row is deleted.
INSERT INTO enrollment_episodes(student_pk,starts_on,entry_reason,created_by)
SELECT s.id, DATE '2000-01-01', 'BESTAND', 'migration'
FROM students s
WHERE NOT EXISTS (SELECT 1 FROM enrollment_episodes e WHERE e.student_pk=s.id);

ALTER TABLE coin_grants ADD COLUMN IF NOT EXISTS episode_id BIGINT REFERENCES enrollment_episodes(id);
ALTER TABLE internships ADD COLUMN IF NOT EXISTS episode_id BIGINT REFERENCES enrollment_episodes(id);

UPDATE coin_grants g SET episode_id=COALESCE(
  (SELECT ep.id FROM enrollment_episodes ep JOIN events e ON e.id=g.event_id
   WHERE ep.student_pk=g.student_pk AND ep.starts_on<=e.end_date
     AND (ep.ends_on IS NULL OR ep.ends_on>=e.end_date)
   ORDER BY ep.starts_on DESC,ep.id DESC LIMIT 1),
  (SELECT ep.id FROM enrollment_episodes ep WHERE ep.student_pk=g.student_pk ORDER BY ep.starts_on,ep.id LIMIT 1)
) WHERE g.episode_id IS NULL;
UPDATE internships i SET episode_id=COALESCE(
  (SELECT ep.id FROM enrollment_episodes ep WHERE ep.student_pk=i.student_pk
   AND ep.starts_on<=i.starts_on AND (ep.ends_on IS NULL OR ep.ends_on>=i.starts_on)
   ORDER BY ep.starts_on DESC,ep.id DESC LIMIT 1),
  (SELECT ep.id FROM enrollment_episodes ep WHERE ep.student_pk=i.student_pk ORDER BY ep.starts_on,ep.id LIMIT 1)
) WHERE i.episode_id IS NULL;

ALTER TABLE coin_grants ALTER COLUMN episode_id SET NOT NULL;
ALTER TABLE internships ALTER COLUMN episode_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_coin_grants_episode ON coin_grants(episode_id,excluded);
CREATE INDEX IF NOT EXISTS idx_internships_episode ON internships(episode_id,finalised);

CREATE OR REPLACE FUNCTION bso_create_initial_episode() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE new_episode BIGINT;
BEGIN
  INSERT INTO enrollment_episodes(student_pk,starts_on,entry_reason,created_by)
  VALUES (NEW.id,CURRENT_DATE,'NEUZUGANG','system') RETURNING id INTO new_episode;
  INSERT INTO student_assignments(episode_id,class_name,grade,track,effective_from,created_by)
  VALUES (new_episode,NEW.class_name,NEW.grade,NEW.track,CURRENT_DATE,'system');
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_student_initial_episode ON students;
CREATE TRIGGER trg_student_initial_episode AFTER INSERT ON students
FOR EACH ROW EXECUTE FUNCTION bso_create_initial_episode();
