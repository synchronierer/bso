# Abnahmematrix T01–T25

Stand 2026-11-03. „Automatisiert“ bedeutet, dass der Kern im Code oder durch einen reproduzierbaren temporären Test geprüft wurde; die fachliche und technische Freigabe bleibt bei Schule/Schul-IT.

| Test | Status | Nachweis / Restpunkt |
|---|---|---|
| T01–T06 | automatisiert | `CoinRulesTest`, exakte BigDecimal-Grenzen und unbekannte Regel |
| T07 | teilweise | additive Event-/Grant-Daten; UI-/Korrekturworkflow noch abzunehmen |
| T08 | implementiert | Äquivalenzprüfung in `/api/grants`; PostgreSQL-Integration mit Katalogdaten noch ausstehend |
| T09 | implementiert | `event_requirements` plus `/api/monitoring/{studentPk}` prüfen Pflichtanzahl, Track und Äquivalenzgruppe; Katalogbefüllung bleibt fachlich |
| T10–T11 | implementiert | `internships`-Checks und eindeutige finale Buchungen; Integrationstest mit realen POSTs ausstehend |
| T12–T13 | implementiert | Wiederholungs-API schließt `student_grade`-Münzen aus, Status/Audit und Rücknahme |
| T14 | implementiert | Flyway V5 mit `enrollment_episodes`/`student_assignments`, Historien-GET und bestätigter Schuljahreswechsel-API für Versetzung, Wiederholung, Austritt und Wiedereintritt |
| T15–T16 | teilweise | Datenbank-Unique/Transaktion und Draft-Restore vorhanden; echte Concurrent-HTTP-Prüfung ausstehend |
| T17 | implementiert | V5-Korrektur-/Status-Endpunkte markieren betroffene Zertifikate `PRUEFUNG_ERFORDERLICH`; erneute Ausgabe erfordert explizite neue Version |
| T18 | teilweise | Klassenzip kann nur gültige Datensätze ausgeben; Fehlerbericht/UI-Entscheidung ist noch abzunehmen |
| T19 | implementiert/getestet | PDF/DOCX-Einzelabruf und Klassenzip in temporärer Compose-Instanz, PDF 1435 Bytes/DOCX 2614 Bytes, Textstichprobe geprüft |
| T20 | getestet | Backup/Restore auf separater Compose-PostgreSQL-Instanz: Flyway-Historie und `app_users` nach Restore vorhanden |
| T21 | implementiert | CSV/XLSX-Vorschau gleicht stabile ID ab und verhindert Dubletten |
| T22–T23 | implementiert | Schuljahreswechsel mit Vorschau, bestätigter Anwendung und Historien-/Episodenpersistenz; fachliche Testdaten/Abnahme noch offen |
| T24 | automatisiert/implementiert | 0,5er-Validierung und bestätigungspflichtige Werte über 20 |
| T25 | teilweise | Status verhindert neue Buchung; Altbuchungen werden noch nicht automatisch als `PRUEFUNG_ERFORDERLICH` markiert |

Zusätzliche Checks: isolierte Docker-Compose-Testinstanz mit PostgreSQL 17 und Flyway 1–5 gestartet, `/api/health` erfolgreich, Migration V5 und neue Tabellen geprüft, Login/Zertifikate/Backup aus vorherigem Lauf nachgewiesen, `git diff --check` vor dem Commit.
