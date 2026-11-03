# Backup und Wiederherstellung

Die Anwendung führt keinen Backup-Job mit Datenbankpasswörtern im Repository aus. Ein geschützter Host-Cronjob ruft `deploy/backup.sh` täglich auf. `BSO_COMPOSE_DIR`, `BSO_BACKUP_DIR` und optional `BSO_STORAGE_DIR` werden außerhalb Git in einer root-lesbaren Konfigurationsdatei gesetzt; das Backupziel muss nur für den Backup-Account zugänglich sein.

Beispiel für `/etc/bso/backup.env` (keine Geheimnisse in Git):

```bash
BSO_COMPOSE_DIR=/srv/bso
BSO_BACKUP_DIR=/srv/bso-backups
BSO_STORAGE_DIR=/srv/bso-storage
```

Täglicher Cron-Eintrag unter einem Konto mit Docker-Berechtigung:

```cron
15 2 * * * . /etc/bso/backup.env; /srv/bso/deploy/backup.sh >>/var/log/bso-backup.log 2>&1
```

Der PostgreSQL-Dump nutzt das Custom-Format, wird mit `umask 077` erstellt und nach 30 Tagen gelöscht. Die Dokument-/Signaturablage wird optional separat als Tarball gesichert. Vor Softwareupdates und Schuljahreswechseln ist derselbe Job zusätzlich manuell auszuführen und die erzeugten Dateien auf Lesbarkeit zu prüfen.

## Restore-Test auf separater Instanz

1. Eine frische PostgreSQL-Instanz in einer isolierten Testumgebung starten; niemals das Produktionsvolume ersetzen.
2. `BSO_COMPOSE_DIR`, `BSO_BACKUP_FILE`, optional `BSO_FILES_BACKUP`, `BSO_STORAGE_DIR` und `CONFIRM_RESTORE=YES` nur in dieser Testumgebung setzen.
3. `deploy/restore.sh` ausführen, anschließend `docker compose up -d` und `/api/health` sowie Login, Schülerhistorie, Zertifikatsversionen und Dateiablage prüfen.
4. Das Ergebnis mit Datum, Backup-Hash und Prüfer im Betriebsprotokoll dokumentieren.

`restore.sh` stoppt nur den BSO-Webdienst der Ziel-Compose-Datei, führt `pg_restore --clean` ausschließlich in der ausdrücklich ausgewählten Ziel-Datenbank aus und entpackt ein optional angegebenes Datei-Backup ausschließlich nach `BSO_STORAGE_DIR`. Es wird weder `docker compose down -v` verwendet noch ein fremder Container angesprochen.
