# Freigabestatus

Stand: 2026-11-03 · Issue #1 · Feature-Branch `feature/issue-1-bso-production-core` · PR #2

## Ergebnis dieses Durchgangs

- Das Datenbankschema wird additiv über Flyway versioniert. Vorhandene Demo-Tabellen werden nicht gelöscht; die Migration verwendet `IF NOT EXISTS` und ergänzt nur fehlende Spalten/Tabellen.
- Münzwerte werden ausschließlich als `BigDecimal`/PostgreSQL-`NUMERIC` in 0,5er-Schritten verarbeitet. Die bestätigten Grenzwerte, Blockwechsel 10→11 und Jahrgang 13 ohne Zertifikat sind als deterministische Regeln getestet.
- Buchungen prüfen serverseitig Veranstaltungsende, Veranstaltungsstatus, Standardwert, Einmaligkeit und Wahlpflicht-Äquivalenz. Eine bestätigte Buchung ist transaktional und mit Benutzer protokolliert.
- Der Java-Container wird in Compose nur auf `127.0.0.1` veröffentlicht. HTTPS bleibt Aufgabe des vorhandenen, von der Schul-IT verwalteten Nginx-Reverse-Proxys.
- XLSX-Importe für Schüler und Veranstaltungen nutzen eine unveränderliche Vorschau, SHA-256-Idempotenz, Duplikatprüfung und explizite Übernahme; CSV bleibt verfügbar.
- Praktika, Klassenwiederholung und Zertifikatsversionen sind serverseitig persistiert; Einzel-PDF/DOCX und Klassen-ZIP sind geschützt authentifizierte Endpunkte.
- Backup-/Restore-Skripte sichern PostgreSQL im Custom-Format, optionale Ablage und löschen nur Backups älter als 30 Tage.

## Noch keine Produktionsfreigabe

Dieser Branch ist nicht für Echtdaten freigegeben. Vor einer gemeinsamen Freigabe durch BSO-Koordination und Schul-IT fehlen bzw. müssen nachgewiesen werden:

- vollständiger T01–T25-Abnahmetest einschließlich Enrollment-/Schuljahreswechsel, Workshops, Pflichtmonitoring, Rückdatierung und echter Konkurrenz-HTTP-Tests; siehe `docs/TEST_MATRIX.md`;
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
- `mvn verify`: in der Agent-Umgebung nicht direkt ausführbar, da Maven und Java nicht installiert sind; `mvn test` im Java-21-Maven-Container: bestanden (6 Tests).
- Docker-Build: bestanden mit Java 21/Maven-Builder. PostgreSQL-Migrationen V1–V4: bestanden in PostgreSQL 17.
- Separater Compose-Integrationslauf: `/api/health`, Flyway, Login, PDF/DOCX-Ausgabe und geschützter Download bestanden.
- Backup/Restore-Test auf dem separaten Compose-Volume: bestanden; Schema-Historie und `app_users` nach Restore vorhanden.
- Vollständiger Testlauf gegen die echte `.env`/Schulserverumgebung: bewusst nicht ausgeführt.
- Echtdaten: nicht verwendet und nicht freigegeben.
