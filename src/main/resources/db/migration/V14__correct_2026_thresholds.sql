-- These values supersede the original 2026/27 threshold draft.
-- Source: BSO Veranstaltungen und Münzen 202627.xlsx, Tabelle1, rows 25-27.
-- Do not alter earlier certificate_version snapshots or coin grants.
DELETE FROM threshold_rules WHERE valid_from=DATE '2026-08-01';
INSERT INTO threshold_rules(grade,track,success,special_success,distinction,valid_from,changed_by)
VALUES
(8,'ohne',10,17,22,DATE '2026-08-01','migration-v14'),
(9,'KA',119,126,133,DATE '2026-08-01','migration-v14'),
(9,'BR',119,126,133,DATE '2026-08-01','migration-v14'),
(9,'V10',16,23,30,DATE '2026-08-01','migration-v14'),
(9,'S1',16,23,30,DATE '2026-08-01','migration-v14'),
(9,'Ü11',16,23,30,DATE '2026-08-01','migration-v14'),
(10,'BR',18,25,32,DATE '2026-08-01','migration-v14'),
(10,'S1',18,25,32,DATE '2026-08-01','migration-v14'),
(10,'Ü11',118,125,132,DATE '2026-08-01','migration-v14'),
(11,'ohne',118,125,132,DATE '2026-08-01','migration-v14'),
(12,'ohne',17,24,31,DATE '2026-08-01','migration-v14');
