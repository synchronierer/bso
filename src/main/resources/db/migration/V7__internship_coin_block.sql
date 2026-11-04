-- Persist the earning block of each internship, independently of later grade changes.
-- Historical records are attributed using the student's assignment at internship end.
ALTER TABLE internships ADD COLUMN IF NOT EXISTS coin_block VARCHAR(12);

UPDATE internships i SET coin_block = CASE
  WHEN COALESCE(
    (SELECT a.grade FROM student_assignments a
     WHERE a.episode_id=i.episode_id AND a.effective_from <= i.ends_on
       AND (a.effective_until IS NULL OR a.effective_until>=i.ends_on)
     ORDER BY a.effective_from DESC, a.id DESC LIMIT 1),
    (SELECT s.grade FROM students s WHERE s.id=i.student_pk)
  ) <= 10 THEN 'SEK_I'
  ELSE 'OBERSTUFE'
END
WHERE coin_block IS NULL;

ALTER TABLE internships ALTER COLUMN coin_block SET NOT NULL;
ALTER TABLE internships ADD CONSTRAINT ck_internship_coin_block CHECK (coin_block IN ('SEK_I','OBERSTUFE'));

-- A mandatory internship may count only once per block in one attendance episode.
-- If old demo data contain conflicting final bookings, migration must stop for
-- operator review instead of silently discarding historical rows.
CREATE UNIQUE INDEX IF NOT EXISTS uq_mandatory_internship_episode_block
 ON internships(episode_id,coin_block)
 WHERE kind='PFLICHT' AND finalised;
