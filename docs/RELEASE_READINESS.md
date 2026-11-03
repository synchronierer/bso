# Freigabestatus

Stand: 2026-11-03 · Issue #1 · Feature-Branch `feature/issue-1-bso-production-core` · PR #2

## Ergebnis dieses Durchgangs

- Das Datenbankschema wird additiv über Flyway versioniert. Vorhandene Demo-Tabellen werden nicht gelöscht; die Migration verwendet `IF NOT EXISTS` und ergänzt nur fehlende Spalten/Tabellen.
- Münzwerte werden ausschließlich als `BigDecimal`/PostgreSQL-`NUMERIC` in 0,5er-Schritten verarbeitet. Die bestätigten Grenzwerte, Blockwechsel 10→11 und Jahrgang 13 ohne Zertifikat sind als deterministische Regeln getestet.
- Buchungen prüfen serverseitig Veranstaltungsende, Veranstaltungsstatus, Standardwert, Einmaligkeit und Wahlpflicht-Äquivalenz. Eine bestätigte Buchung ist transaktional und mit Benutzer protokolliert.
- Der Java-Container wird in Compose nur auf `127.0.0.1` veröffentlicht. HTTPS bleibt Aufgabe des vorhandenen, von der Schul-IT verwalteten Nginx-Reverse-Proxys.
- XLSX-Importe für Schüler und Veranstaltungen nutzen eine unveränderliche Vorschau, SHA-256-Idempotenz, Duplikatprüfung und explizite Übernahme; CSV bleibt verfügbar.
- Praktika, Klassenwiederholung und Zertifikatsversionen sind serverseitig persistiert; Einzel-PDF/DOCX und Klassen-ZIP sind geschützt authentifizierte Endpunkte.
- Schuljahreswechsel und Bildungsganghistorie werden additiv in Episoden/Zuordnungen geführt; Vorschau und bestätigte Anwendung decken Versetzung, Wiederholung, Austritt und Wiedereintritt ab.
- Münzbuchungen und Praktika tragen jetzt zwingend `episode_id`; vorhandene Datensätze werden datumsbezogen rückverknüpft. Neue Schüler erhalten per Datenbanktrigger automatisch eine eigene Startepisode. Wiedereintrittssalden beginnen nachweisbar bei null.
- Pflichtveranstaltungsanforderungen können pro Schuljahr/Jahrgang/Track mit Mindestanzahl und Äquivalenzgruppe überwacht werden. Veranstaltungs-Korrekturen und Absagen markieren abhängige Zertifikate automatisch zur Prüfung.
- Backup-/Restore-Skripte sichern PostgreSQL im Custom-Format, optionale Ablage und löschen nur Backups älter als 30 Tage.

## Noch keine Produktionsfreigabe

Dieser Branch ist nicht für Echtdaten freigegeben. Vor einer gemeinsamen Freigabe durch BSO-Koordination und Schul-IT fehlen bzw. müssen nachgewiesen werden:

- fachlicher Abschluss der T01–T25-Matrix, insbesondere Praktikums-POSTs, vollständiger Workshopkatalog, visuelle Zertifikatsprüfung und vollständige Berechtigungsmatrix; technische Kernnachweise stehen in `docs/TEST_MATRIX.md`;
- produktionsnahe Backup-/Restore-Abnahme mit den tatsächlich von der Schul-IT verwendeten Dokument-/Signaturpfaden;
- Sicherheits-/Datenschutzprüfung einschließlich Nginx-Vertrauen, Cookie-/Session-Prüfung, Upload-Schutz, Aufbewahrung und Löschkonzept;
- fachliche Prüfung der Veranstaltungskataloge sowie Freigabe von Schulbriefkopf und Signatur; die generierten Dokumente enthalten die fiktiven Veranstaltungs-/Praktikumslisten und müssen visuell durch die Schule abgenommen werden;
- Installation auf einer separaten Testinstanz mit fiktiven Daten und dokumentiertem Rollback.

## Update und Rollback

1. Vor dem Update Compose-Datei, `.env`, Datenbank und Dokumentenablage sichern; niemals `docker compose down -v` verwenden.
2. Migration und Anwendung zunächst auf einer separaten Testinstanz mit einer Wiederherstellungskopie prüfen.
3. Für das Update ausschließlich `docker compose up -d --build` mit dem geprüften Branch verwenden und anschließend Healthcheck, Login, Importvorschau und Buchung testen.
4. Bei einem Fehler Anwendung auf das vorherige Image zurückstellen. Eine bereits ausgeführte additive Migration wird nicht durch Löschen von Tabellen rückgängig gemacht; bei Bedarf Backup in einer separaten Instanz wiederherstellen und die Datenabweichung fachlich prüfen.

## Teststatus

- `git diff --check`: bestanden.
- `mvn verify` im Java-21-Maven-Container: bestanden (6 Tests).
- Docker-Build: bestanden mit Java 21/Maven-Builder. PostgreSQL-Migrationen V1–V6: bestanden in einer frischen isolierten PostgreSQL-17-Compose-Instanz.
- Separater Compose-Integrationslauf: `/api/health`, Flyway, Login, PDF/DOCX-Ausgabe und geschützter Download bestanden.
- Backup/Restore-Test auf dem separaten Compose-Volume: bestanden; Schema-Historie und `app_users` nach Restore vorhanden.
- RC1-HTTP-Test: zwei parallele Münzbuchungen lieferten genau eine Annahme und eine 400-Ablehnung; zwei parallele Draft-Speicherungen hinterließen genau einen Draft; fehlender CSRF-Token wurde mit 403 abgewiesen.
- RC1-Dateitest: PDF/DOCX mit fiktiven Daten erzeugt, DOCX-Inhalte extrahiert und Datenbank plus Dateiarchiv auf isolierter Instanz wiederhergestellt.
- Vollständiger Testlauf gegen die echte `.env`/Schulserverumgebung: bewusst nicht ausgeführt.
- Echtdaten: nicht verwendet und nicht freigegeben.
