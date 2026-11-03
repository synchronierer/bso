# AGENTS.md – Arbeitsanweisungen für Codex

## Hauptauftrag

Arbeite an der Java-/Spring-Boot-Anwendung im Repository `synchronierer/bso` zur BSO-Münz- und Zertifikatverwaltung der IGS Am Nanstein.

**Lies unbedingt zuerst:**
1. [docs/CODEX_MASTER_AUFTRAG.md](docs/CODEX_MASTER_AUFTRAG.md) – vollständiger Implementierungsauftrag mit Priorität und Tests.
2. [docs/PFLICHTENHEFT.md](docs/PFLICHTENHEFT.md) – fachlich verbindliche Regeln aus 115 abgestimmten Entscheidungen.
3. [docs/HTTPS.md](docs/HTTPS.md) – Hintergrund HTTPS-Proxy; aktuelle Umgebung verwendet vorhandenen externen Nginx, **nicht Caddy**.

Die später im Pflichtenheft ausdrücklich geänderten Entscheidungen gehen früheren Regeln vor. Fachregeln nicht eigenmächtig uminterpretieren.

## Arbeitsweise

- **Implementieren, nicht nur planen.** In möglichst einem zusammenhängenden Durchgang nach Priorität, keine fortlaufenden Rückfragen bei lösbaren Detailentscheidungen.
- Bestehende Datenbank, Docker-Volumes und andere Container des Schulservers schützen. Keine automatischen Remote-Deployments, keine destruktiven Schemaoperationen und **kein `docker compose down -v`**.
- Alle Änderungen in einem Feature-Branch und Pull Request mit nachvollziehbaren Tests zusammenfassen.
- Nur fiktive Schülerdaten in Tests und Git; keine Personenakten, Schulzugangsdaten, internen IPs, geheimen Zertifikatsschlüssel, Signaturgrafiken oder Datenbankdumps veröffentlichen.
- Keine Eröffnung des HTTP-Ports im LAN: Java-Container nur auf `127.0.0.1` binden; externe HTTPS-Verbindung über vorhandenes, von der Schul-IT konfiguriertes Nginx. Sicherheit und Datenschutz vor Produktivnahme nachweisen.
- Benutzeroberfläche deutsch und besonders einfach für Lehrkräfte: CSV/XLSX-Import mit Vorschau, Klassenraster, automatische Entwürfe, eine bestätigte Sammelbuchung.
- Java 21, Spring Boot, PostgreSQL, exakte `BigDecimal`-Münzen, überprüfbare Transaktionen, Flyway/Liquibase, ausreichende Regressionstests und CI.
- Bei Unsicherheit: sicherere, datenerhaltende Variante mit Dokumentation im PR; fehlende Fachgrundlagen als offene Punkte aufzeichnen.
- **Keine Echtdatenfreigabe behaupten:** `docs/RELEASE_READINESS.md` pflegen; gemeinsamer fachlicher und technischer Freigabebeschluss ist Pflicht.

## Fertigstellung

Führe `mvn verify`, Datenbank-/Integrations- und Docker-Build-Tests aus, dokumentiere Ergebnisse/Blocker, liefere Update-/Rollback-/Backup-/Restore-Anleitung. Bereite einen PR für Review vor, statt ungetestete Änderungen direkt im laufenden `main` auszurollen.
