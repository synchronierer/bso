#!/usr/bin/env bash
set -Eeuo pipefail

: "${BSO_COMPOSE_DIR:?BSO_COMPOSE_DIR must point to the checked-out deployment directory}"
: "${BSO_BACKUP_FILE:?BSO_BACKUP_FILE must name a reviewed custom-format pg_dump}"
if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo 'Restore ist destruktiv für die Ziel-Datenbank. Setze CONFIRM_RESTORE=YES nach Prüfung des Backups.' >&2
  exit 2
fi
[[ -r "$BSO_BACKUP_FILE" ]] || { echo 'Backup nicht lesbar' >&2; exit 2; }
if [[ -n "${BSO_FILES_BACKUP:-}" ]]; then
  : "${BSO_STORAGE_DIR:?BSO_STORAGE_DIR must identify the isolated document/signature target}"
  [[ -r "$BSO_FILES_BACKUP" ]] || { echo 'Datei-Backup nicht lesbar' >&2; exit 2; }
fi
cd "$BSO_COMPOSE_DIR"
docker compose stop web
docker compose exec -T db pg_restore -U "${POSTGRES_USER:-bso}" -d "${POSTGRES_DB:-bso}" --clean --if-exists --exit-on-error < "$BSO_BACKUP_FILE"
if [[ -n "${BSO_FILES_BACKUP:-}" ]]; then
  mkdir -p "$BSO_STORAGE_DIR"
  tar -xzf "$BSO_FILES_BACKUP" -C "$BSO_STORAGE_DIR"
fi
docker compose start web
printf 'Restore abgeschlossen: %s\n' "$BSO_BACKUP_FILE"
