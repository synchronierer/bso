# Abnahmematrix T01–T25

Stand 2026-11-03. „Automatisiert“ bedeutet, dass der Kern im Code oder durch einen reproduzierbaren temporären Test geprüft wurde; die fachliche und technische Freigabe bleibt bei Schule/Schul-IT.

| Test | Status | Nachweis / Restpunkt |
|---|---|---|
| T01–T06 | automatisiert | `CoinRulesTest`, exakte BigDecimal-Grenzen und unbekannte Regel |
| T07 | teilweise | additive Event-/Grant-Daten; UI-/Korrekturworkflow noch abzunehmen |
| T08 | implementiert | Äquivalenzprüfung in `/api/grants`; PostgreSQL-Integration mit Katalogdaten noch ausstehend |
| T09 | offen | Workshop-Katalog und Mindestüberwachung noch nicht vollständig modelliert |
| T10–T11 | implementiert | `internships`-Checks und eindeutige finale Buchungen; Integrationstest mit realen POSTs ausstehend |
| T12–T13 | implementiert | Wiederholungs-API schließt `student_grade`-Münzen aus, Status/Audit und Rücknahme |
| T14 | offen | vollständige Enrollment-/Track-Historie fehlt |
| T15–T16 | teilweise | Datenbank-Unique/Transaktion und Draft-Restore vorhanden; echte Concurrent-HTTP-Prüfung ausstehend |
| T17 | offen | Zertifikate werden versioniert, automatische Prüfung-Status-Markierung nach Korrektur fehlt |
| T18 | teilweise | Klassenzip kann nur gültige Datensätze ausgeben; Fehlerbericht/UI-Entscheidung ist noch abzunehmen |
| T19 | implementiert/getestet | PDF/DOCX-Einzelabruf und Klassenzip in temporärer Compose-Instanz, PDF 1435 Bytes/DOCX 2614 Bytes, Textstichprobe geprüft |
| T20 | getestet | Backup/Restore auf separater Compose-PostgreSQL-Instanz: Flyway-Historie und `app_users` nach Restore vorhanden |
| T21 | implementiert | CSV/XLSX-Vorschau gleicht stabile ID ab und verhindert Dubletten |
| T22–T23 | offen | vollständiger Schuljahreswechsel/Episode fehlt |
| T24 | automatisiert/implementiert | 0,5er-Validierung und bestätigungspflichtige Werte über 20 |
| T25 | teilweise | Status verhindert neue Buchung; Altbuchungen werden noch nicht automatisch als `PRUEFUNG_ERFORDERLICH` markiert |

Zusätzliche Checks: Docker-Compose-Testinstanz mit PostgreSQL 17 und Flyway 1–4 gestartet, `/api/health` erfolgreich, Login erfolgreich, Zertifikate erzeugt, `git diff --check` ausstehend bis zum finalen Commit.
