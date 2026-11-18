# HTTPS für die BSO-Demo auf dem Schulserver

**Wichtiger Hinweis:** HTTPS verschlüsselt die Browser-Verbindung. Es macht die derzeitige BSO-Demo **nicht** für die Verarbeitung realer Schülerdaten bereit. Die Anwendung enthält noch unvollständige fachliche Prüfungen, Protokollierung, Rechteprüfung und Betriebsprozesse. Erst nach Test, Datenschutzprüfung und gemeinsamer Freigabe durch BSO-Koordination und Schul-IT dürfen personenbezogene Schülerdaten erfasst werden.

## 1. Voraussetzungen prüfen

- Der richtige interne BSO-Server ist **172.26.33.19**, die Demo lief bislang auf Port **8081**.
- Port **443/TCP** ist auf dem Server frei und durch die Schul-IT für die vorgesehenen Schulgeräte freigegeben.
- Docker Compose V2 ist installiert.
- Die Schul-IT verwaltet den Vertrauensspeicher der Browser/Schul-PCs.
- Bestehende `.env` und PostgreSQL-Volumes bleiben erhalten.

Vorher auf dem Server:

```bash
cd /opt/bso
docker compose ps
sudo ss -ltnp '( sport = :443 )'
cp compose.yaml compose.yaml.backup
```

Bei belegtem 443 **keinen fremden Dienst stoppen**; die Proxy-Konfiguration muss dann über den vorhandenen Reverse Proxy der Schul-IT erfolgen.

## 2. Dateien aktualisieren

```bash
cd /opt/bso
git status --short
git pull --ff-only
```

Bei lokalen Änderungen an `compose.yaml` erst prüfen und kontrolliert zusammenführen, nicht einfach überschreiben.

## 3. Vorhandenen Nginx-Reverse-Proxy verwenden

Die laufende Umgebung verwendet den vorhandenen, von der Schul-IT verwalteten
Nginx-Reverse-Proxy. Dieser Branch startet keinen Caddy und verändert keine
Nginx-Konfiguration. Nginx muss intern ausschließlich auf den Loopback-Port des
Webcontainers weiterleiten, zum Beispiel:

```nginx
location / {
    proxy_pass http://127.0.0.1:8081;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port 443;
}
```

## 4. Zertifikat und Vertrauensstellung

Das TLS-Zertifikat und die Vertrauensstellung werden ausschließlich durch die
Schul-IT am bestehenden Nginx verwaltet. Private Schlüssel bleiben dort und
werden nicht in dieses Repository, in Compose-Variablen oder in Containerlogs
übernommen. Ein Export aus einem Caddy-Container ist für diese Umgebung nicht
vorgesehen.

Prüfen: `https://172.26.33.19` auf einem Schul-PC mit installierter Root-CA. Das Vorhängeschloss sollte ohne Warnung erscheinen.

## 5. HTTP-Zugriff schließen

Der bisherige BSO-Web-Port `8081` kann HTTPS umgehen, wenn er weiter im Schulnetz veröffentlicht wird. Die Schul-IT muss in `compose.yaml` beim Dienst `web` ändern:

```yaml
ports:
  - "127.0.0.1:${BSO_PORT:-8081}:8080"
```

Danach die Dienste ohne Volumelöschung neu erstellen:

```bash
docker compose up -d --build
```

**Niemals** `docker compose down -v` ausführen: Das löscht Datenbankvolumes.

## 6. Grenzen

- Diese Konfiguration dient zuerst technischen HTTPS-Tests **mit fiktiven Daten**.
- Bis Schul-IT/Datenschutzbeauftragte geprüft und freigegeben haben, **keine echten Schülerdaten**.
- Der private CA-Schlüssel verbleibt geschützt im Docker-Volume. Backup und Berechtigungen sind gesondert zu prüfen.
- Am Produktivsystem sind `Secure`-Cookie-Einstellungen, Weiterleitung von Proxy-Headern, Schutz vor Session-Fixation, CSRF-Prüfung, Audit, Backup-/Restore-Test und dauerhafte Zugriffskontrolle zu prüfen.
- Bei bereits vorhandenem schulischem Reverse Proxy sollte die Schul-IT diesen stattdessen integrieren; ein zweiter Caddy ist nicht nötig.
