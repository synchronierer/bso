# Freigabestatus

Stand: 2026-11-03 · Issue #1 · Feature-Branch `feature/issue-1-bso-production-core`

## Ergebnis dieses Durchgangs

- Das Datenbankschema wird additiv über Flyway versioniert. Vorhandene Demo-Tabellen werden nicht gelöscht; die Migration verwendet `IF NOT EXISTS` und ergänzt nur fehlende Spalten/Tabellen.
- Münzwerte werden ausschließlich als `BigDecimal`/PostgreSQL-`NUMERIC` in 0,5er-Schritten verarbeitet. Die bestätigten Grenzwerte, Blockwechsel 10→11 und Jahrgang 13 ohne Zertifikat sind als deterministische Regeln getestet.
- Buchungen prüfen serverseitig Veranstaltungsende, Veranstaltungsstatus, Standardwert, Einmaligkeit und Wahlpflicht-Äquivalenz. Eine bestätigte Buchung ist transaktional und mit Benutzer protokolliert.
- Der Java-Container wird in Compose nur auf `127.0.0.1` veröffentlicht. HTTPS bleibt Aufgabe des vorhandenen, von der Schul-IT verwalteten Nginx-Reverse-Proxys.

## Noch keine Produktionsfreigabe

Dieser Branch ist nicht für Echtdaten freigegeben. Vor einer gemeinsamen Freigabe durch BSO-Koordination und Schul-IT fehlen bzw. müssen nachgewiesen werden:

- vollständiger T01–T25-Abnahmetest einschließlich PostgreSQL-Integration, Klassenwiederholung, Schuljahreswechsel, XLSX-Import und PDF/DOCX-Ausgabe;
- Backup-/Restore-Test für Datenbank, Dokumentenablage und geschützte Signaturablage;
- Sicherheits-/Datenschutzprüfung einschließlich Nginx-Vertrauen, Cookie-/Session-Prüfung, Upload-Schutz, Aufbewahrung und Löschkonzept;
- fachliche Prüfung der Veranstaltungskataloge, Schulbriefkopf- und Signaturfreigabe;
- Installation auf einer separaten Testinstanz mit fiktiven Daten und dokumentiertem Rollback.

## Update und Rollback

1. Vor dem Update Compose-Datei, `.env`, Datenbank und Dokumentenablage sichern; niemals `docker compose down -v` verwenden.
2. Migration und Anwendung zunächst auf einer separaten Testinstanz mit einer Wiederherstellungskopie prüfen.
3. Für das Update ausschließlich `docker compose up -d --build` mit dem geprüften Branch verwenden und anschließend Healthcheck, Login, Importvorschau und Buchung testen.
4. Bei einem Fehler Anwendung auf das vorherige Image zurückstellen. Eine bereits ausgeführte additive Migration wird nicht durch Löschen von Tabellen rückgängig gemacht; bei Bedarf Backup in einer separaten Instanz wiederherstellen und die Datenabweichung fachlich prüfen.

## Teststatus

- `git diff --check`: bestanden.
- `mvn verify`: in der Agent-Umgebung nicht direkt ausführbar, da Maven und Java nicht installiert sind; `mvn test` im Java-21-Maven-Container: bestanden (4 Tests).
- Docker-Build: bestanden mit Java 21/Maven-Builder. PostgreSQL-Migration: bestanden in einer frischen temporären PostgreSQL-17-Instanz.
- Vollständiger Docker-Compose-Integrationslauf mit der echten `.env`/Schulserverumgebung: bewusst nicht ausgeführt.
- Echtdaten: nicht verwendet und nicht freigegeben.
