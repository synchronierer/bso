# BSO RC1 – Installation und Rollback

Diese Anleitung gilt nur für eine separat vorbereitete Schulserver-Installation. Sie verändert weder `/opt/bso` automatisch noch andere Compose-Projekte. Produktive Zugangsdaten werden ausschließlich außerhalb von Git gesetzt.

## Vorabprüfung

1. Freigabe durch BSO-Koordination, Schul-IT und Datenschutzverantwortliche dokumentieren.
2. Wartungsfenster und zuständige Rückfallperson festlegen.
3. Compose-Datei, `.env`, PostgreSQL und die konfigurierte Dokument-/Signaturablage sichern:

   ```bash
   . /etc/bso/backup.env
   BSO_COMPOSE_DIR=/opt/bso BSO_BACKUP_DIR=/srv/bso-backups BSO_STORAGE_DIR=/srv/bso-storage deploy/backup.sh
   ```

4. Backup-Dateien mit `sha256sum` prüfen und den Restore auf einer getrennten Testinstanz durchführen. Niemals `docker compose down -v` verwenden.

## Migration und Start

1. RC1 auf einer Kopie des geprüften Branches bereitstellen; `.env` und Geheimnisse nicht aus Git übernehmen.
2. `docker compose config` prüfen: Web-Port darf nur `127.0.0.1` exponieren; Datenbank bleibt im privaten Compose-Netz.
3. `docker compose up -d --build` ausführen. Flyway muss V1–V6 ohne Fehler anwenden.
4. Healthcheck und Anmeldung prüfen: `curl --fail http://127.0.0.1:8081/api/health`.
5. Mit einem fiktiven Testkonto CSRF, Login, Schülerhistorie, Importvorschau, Entwurf, Buchung und PDF/DOCX-Ausgabe prüfen. Erst danach fachlich freigegebene Daten importieren.

## Rollback

Bei Anwendungsfehlern zunächst den Dienst auf das vorherige Image zurückstellen. Migrationen werden nicht durch Löschen von Tabellen rückgängig gemacht. Wenn die Datenbank betroffen ist, Anwendung stoppen und das geprüfte Backup ausschließlich in einer separaten Wiederherstellungsinstanz einspielen; ein produktiver Restore braucht eine ausdrücklich protokollierte Schul-IT-Entscheidung.

Nach jedem Rollback Healthcheck, Login, Flyway-Stand, Zertifikatsdownload und Datenbank-/Dateiablage prüfen. Abweichungen und die verwendete Backup-Hashsumme im Betriebsprotokoll festhalten.

## RC1-Abschlusskriterien

- Flyway V1–V6, `mvn verify`, Docker-Build und Compose-Start erfolgreich.
- Episode-Isolation: Austritt/Wiedereintritt erzeugt getrennte Salden; alte Buchungen bleiben historisch sichtbar.
- T01–T25 sowie parallele HTTP-, CSRF- und Rollenprüfungen mit fiktiven Daten nachvollziehbar dokumentiert.
- Datenbank, Zertifikatsdateien und geschützte Signaturablage in einer isolierten Instanz wiederhergestellt.
- Fachliche, technische und datenschutzrechtliche Freigabe getrennt unterschrieben.
