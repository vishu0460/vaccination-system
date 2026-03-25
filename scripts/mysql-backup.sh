#!/usr/bin/env bash

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups/mysql}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-vaccination_db}"
DB_USER="${DB_USER:-appuser}"
DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required for backups}"

mkdir -p "${BACKUP_DIR}"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${BACKUP_DIR}/${DB_NAME}-${timestamp}.sql.gz"

MYSQL_PWD="${DB_PASSWORD}" mysqldump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --single-transaction \
  --quick \
  --routines \
  --events \
  --set-gtid-purged=OFF \
  "${DB_NAME}" | gzip -9 > "${backup_file}"

find "${BACKUP_DIR}" -type f -name "${DB_NAME}-*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete

echo "Backup written to ${backup_file}"
