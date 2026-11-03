# BSO-Münzverwaltung – IGS Am Nanstein

**Status: erste technische DEMO, ausdrücklich nicht produktionsreif.** Das vollständige [Pflichtenheft](docs/PFLICHTENHEFT.md) beschreibt die erst noch umzusetzenden Anforderungen.

## Demo: Was jetzt funktioniert?

Java-21-/Spring-Boot-Webserver, PostgreSQL-Datenbank, Oberfläche zur Erfassung **fiktiver** Schüler, Veranstaltungen und einmaliger Münzvergaben je Event/Schüler sowie eine einfache Klassenübersicht. Daten bleiben nach Containerneustart erhalten.

**Was noch fehlt:** HTTPS, Datenschutzfreigabe, Schülerimporte, Pflichtveranstaltungen, Praktika, Äquivalenzgruppen, Schuljahreswechsel, getrennte Münzblöcke, Klassenwiederholungen, Entwurfsspeicherung, Sperren, Audit, Zertifikate, Berichte und Backups. Die angezeigten Münzen sind derzeit reine Summen, keine verbindlichen BSO-Kontostände.

## Installation auf Linux mit Docker Compose

Voraussetzungen: Docker Engine und Docker Compose Plugin, Schul-IT-Zugriff auf den Linux-Server, ausreichend freier Speicher, interne Firewall.

```bash
git clone https://github.com/synchronierer/bso.git
cd bso
cp .env.example .env
# .env bearbeiten: DB_PASSWORD und ADMIN_INITIAL_PASSWORD durch
# zwei unterschiedliche lange Zufallspasswörter ersetzen.
# Wichtig: Beim allerersten Start wird damit das Konto 'admin' angelegt.
docker compose up -d --build
docker compose ps
```

Im Browser im **internen Netz**: `http://<INTERNE-SERVER-IP>:8081`

**Achtung:** Der Dienst hat nun eine Anmeldung, ist aber noch **unverschlüsselt (HTTP)** und nicht für den Echtbetrieb abgesichert. Ausschließlich mit erfundenen Daten nutzen. Port 8080 über Firewall ausschließlich auf einen engen Kreis befugter Testgeräte begrenzen, nicht ins Internet routen. Die geplante HTTPS-Absicherung per Zertifikat mit IP-SAN wird vor dem Echtbetrieb ergänzt.

Logs ansehen:
```bash
docker compose logs -f web
```

Demo stoppen (Datenbank bleibt erhalten):
```bash
docker compose down
```

Demo samt Daten **unwiderruflich löschen**:
```bash
docker compose down -v
```

## Entwicklungsziele

Die Umsetzung des vollständigen Pflichtenhefts erfolgt in weiteren Schritten. Die jetzige Demo dient ausschließlich dazu, **Webserver, Container, PostgreSQL und Browseroberfläche** gemeinsam zu prüfen. Erst nach implementierter Authentifizierung, HTTPS, Sicherheitsprüfung und fachlicher Testabdeckung dürfen echte personenbezogene Daten verarbeitet werden.

**Keine Passwörter, Signaturen oder echten Schülerdaten nach GitHub committen.**

## Update der bestehenden Demo (ohne Datenverlust)

1. Vorher Datenbanksicherung erstellen und aufbewahren (Datenbankvolume **niemals** mit `down -v` löschen).
2. Im bereits vorhandenen `.env` zusätzlich `ADMIN_INITIAL_PASSWORD=<eigenes langes Passwort>` und `BSO_PORT=8081` setzen. Die bestehende Zeile `DB_PASSWORD` **nicht ändern**.
3. `git pull --ff-only` ausführen. Lokale Änderungen an `compose.yaml` (z. B. Port 8081) können das Pull blockieren: diese vorher sichern und sauber übernehmen, nicht pauschal zurücksetzen.
4. `docker compose up -d --build`, `docker compose ps`, `docker compose logs --tail=100 web`.
5. `http://<INTERNE-SERVER-IP>:8081/login.html`, Benutzername `admin`, Passwort aus `.env`. Admin kann Lehrkräfte anlegen und deaktivieren.

**Sitzung:** 30 Minuten Inaktivität; UI-Warnung ab Minute 28. Die serverseitige Sitzungszeit beträgt 30 Minuten. Passwörter liegen ausschließlich gehasht vor. Das Startpasswort aus `.env` wird nur beim **erstmaligen** Anlegen des Administrationskontos verwendet.

**Sicherheitsvorbehalt:** HTTP überträgt Sitzungsinformationen unverschlüsselt. Noch keine echten Schülerdaten eingeben, auch mit Login nicht. Vor Echtbetrieb HTTPS auf der internen IP, Backup/Restore, Audit und Datenschutzfreigabe ergänzen.
