# Abnahmematrix T01–T25

Stand 2026-11-03 RC1. „Automatisiert“ bedeutet, dass der Kern im Code oder durch einen reproduzierbaren isolierten Test geprüft wurde; die fachliche und technische Freigabe bleibt bei Schule/Schul-IT.

| Test | Status | Nachweis / Restpunkt |
|---|---|---|
| T01–T06 | automatisiert | `CoinRulesTest`, exakte BigDecimal-Grenzen und unbekannte Regel |
| T07 | implementiert | additive Event-/Grant-Daten und protokollierte Korrekturroute; fachliche UI-Abnahme bleibt offen |
| T08 | implementiert | Äquivalenzprüfung in `/api/grants` und episode-isoliertes Ledger |
| T09 | implementiert | `event_requirements` plus `/api/monitoring/{studentPk}` prüfen Pflichtanzahl, Track und Äquivalenzgruppe; Katalogbefüllung bleibt fachlich |
| T10–T11 | teilweise | `internships`-Checks und eindeutige finale Buchungen; HTTP-Fachtest noch nicht separat ausgeführt |
| T12–T13 | implementiert | Wiederholungs-API schließt nur Münzen der offenen Episode und wiederholten Stufe aus, Status/Audit und Rücknahme |
| T14 | implementiert/getestet | Flyway V5/V6, Episoden-FK/NOT-NULL, Trigger für Neuzugänge; Austritt/Wiedereintritt isoliert alten Saldo im HTTP-/PostgreSQL-Test |
| T15–T16 | implementiert/getestet | parallele HTTP-Buchung ergibt 201/400 und genau einen Ledger-Eintrag; parallele Entwurfsspeicherung ergibt genau einen Draft |
| T17 | implementiert | Korrektur-/Status-Endpunkte markieren betroffene Zertifikate `PRUEFUNG_ERFORDERLICH`; erneute Ausgabe erfordert explizite neue Version |
| T18 | teilweise | Klassenzip kann nur gültige Datensätze ausgeben; Fehlerbericht/UI-Entscheidung ist noch abzunehmen |
| T19 | teilweise getestet | Realistische fiktive PDF/DOCX-Einzeldateien erzeugt; DOCX-Inhalte und PDF-Bytes geprüft, formale PDFBox-Seitenassertion/visuelle Schulabnahme verbleibt |
| T20 | implementiert/getestet | isolierter Restore von PostgreSQL plus Dateiarchiv für Zertifikats-/Signaturablage; Flyway V1–V6 und Healthcheck danach erfolgreich |
| T21 | implementiert | CSV/XLSX-Vorschau gleicht stabile ID ab und verhindert Dubletten |
| T22–T23 | implementiert/getestet | Schuljahreswechsel mit Vorschau, bestätigter Anwendung und Historien-/Episodenpersistenz; fachliche Importvollständigkeit bleibt offen |
| T24 | automatisiert/implementiert | 0,5er-Validierung und bestätigungspflichtige Werte über 20 |
| T25 | implementiert | Event-Abbruchroute blockiert neue Buchungen und markiert abhängige Zertifikate automatisch als `PRUEFUNG_ERFORDERLICH` |

Zusätzliche Checks: isolierte Docker-Compose-Testinstanz mit PostgreSQL 17 und Flyway 1–6 gestartet, `/api/health` erfolgreich, Episode-FK/NOT-NULL geprüft, parallele Buchung und Draft-Upsert, CSRF-403, PDF/DOCX mit fiktiven Daten und vollständiger Daten-/Datei-Restore nachgewiesen. CI muss den finalen Commit noch ausführen.
