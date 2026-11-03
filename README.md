# BSO-Münzverwaltung – IGS Am Nanstein

**Status: erste technische DEMO, ausdrücklich nicht produktionsreif.** Das vollständige [Pflichtenheft](docs/PFLICHTENHEFT.md) beschreibt die erst noch umzusetzenden Anforderungen.

## Demo: Was jetzt funktioniert?

Java-21-/Spring-Boot-Webserver, PostgreSQL-Datenbank, Oberfläche zur Erfassung **fiktiver** Schüler, Veranstaltungen und einmaliger Münzvergaben je Event/Schüler sowie eine einfache Klassenübersicht. Daten bleiben nach Containerneustart erhalten.

**Was noch fehlt:** Benutzeranmeldung und Rollen, HTTPS, Datenschutzfreigabe, Schülerimporte, Pflichtveranstaltungen, Praktika, Äquivalenzgruppen, Schuljahreswechsel, getrennte Münzblöcke, Klassenwiederholungen, Entwurfsspeicherung, Sperren, Audit, Zertifikate, Berichte und Backups. Die angezeigten Münzen sind derzeit reine Summen, keine verbindlichen BSO-Kontostände.

## Installation auf Linux mit Docker Compose

Voraussetzungen: Docker Engine und Docker Compose Plugin, Schul-IT-Zugriff auf den Linux-Server, ausreichend freier Speicher, interne Firewall.

```bash
git clone https://github.com/synchronierer/bso.git
cd bso
cp .env.example .env
# .env bearbeiten: DB_PASSWORD durch zufällig gewähltes langes Passwort ersetzen.
docker compose up -d --build
docker compose ps
```

Im Browser im **internen Netz**: `http://<INTERNE-SERVER-IP>:8080`

**Achtung:** Das ist bewusst ein **unverschlüsselter, nicht authentifizierter Demo-Dienst**. Ausschließlich mit erfundenen Daten nutzen. Port 8080 über Firewall ausschließlich auf einen engen Kreis befugter Testgeräte begrenzen, nicht ins Internet routen. Die geplante HTTPS-Absicherung per Zertifikat mit IP-SAN wird vor dem Echtbetrieb ergänzt.

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
