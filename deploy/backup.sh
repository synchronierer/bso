#!/usr/bin/env bash
set -Eeuo pipefail

# Configure these outside Git (for example /etc/bso/backup.env).
: "${BSO_COMPOSE_DIR:?BSO_COMPOSE_DIR must point to the checked-out deployment directory}"
: "${BSO_BACKUP_DIR:?BSO_BACKUP_DIR must be an access-controlled backup directory}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BSO_BACKUP_DIR"
umask 077
archive="$BSO_BACKUP_DIR/bso-$timestamp.dump"
files="$BSO_BACKUP_DIR/bso-$timestamp-files.tar.gz"

cd "$BSO_COMPOSE_DIR"
docker compose exec -T db pg_dump -U "${POSTGRES_USER:-bso}" -d "${POSTGRES_DB:-bso}" --format=custom > "$archive"
if [[ -n "${BSO_STORAGE_DIR:-}" && -d "$BSO_STORAGE_DIR" ]]; then
  tar -czf "$files" -C "$BSO_STORAGE_DIR" .
fi
find "$BSO_BACKUP_DIR" -type f -name 'bso-*.dump' -mtime +30 -delete
find "$BSO_BACKUP_DIR" -type f -name 'bso-*.tar.gz' -mtime +30 -delete
printf 'Backup erstellt: %s\n' "$archive"
