#!/usr/bin/env bash
set -Eeuo pipefail

: "${BSO_COMPOSE_DIR:?BSO_COMPOSE_DIR must point to the checked-out deployment directory}"
: "${BSO_BACKUP_FILE:?BSO_BACKUP_FILE must name a reviewed custom-format pg_dump}"
if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo 'Restore ist destruktiv für die Ziel-Datenbank. Setze CONFIRM_RESTORE=YES nach Prüfung des Backups.' >&2
  exit 2
fi
[[ -r "$BSO_BACKUP_FILE" ]] || { echo 'Backup nicht lesbar' >&2; exit 2; }
cd "$BSO_COMPOSE_DIR"
docker compose stop web
docker compose exec -T db pg_restore -U "${POSTGRES_USER:-bso}" -d "${POSTGRES_DB:-bso}" --clean --if-exists --exit-on-error < "$BSO_BACKUP_FILE"
docker compose start web
printf 'Restore abgeschlossen: %s\n' "$BSO_BACKUP_FILE"
