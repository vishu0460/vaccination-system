#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PATH="$REPO_ROOT/backend"
ENV_FILE="$REPO_ROOT/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo ".env file not found at $ENV_FILE" >&2
  exit 1
fi

SOURCE_DB_URL_VALUE="$(grep '^DB_URL=' "$ENV_FILE" | head -n 1 | cut -d= -f2-)"
SOURCE_DB_USERNAME_VALUE="$(grep '^DB_USERNAME=' "$ENV_FILE" | head -n 1 | cut -d= -f2-)"
SOURCE_DB_PASSWORD_VALUE="$(grep '^DB_PASSWORD=' "$ENV_FILE" | head -n 1 | cut -d= -f2-)"

if [[ -z "${SOURCE_DB_URL_VALUE:-}" ]]; then
  echo "Source .env does not contain DB_URL for the H2 database." >&2
  exit 1
fi

if [[ "${SOURCE_DB_URL_VALUE,,}" != jdbc:h2:* ]]; then
  echo "Source .env DB_URL is not an H2 URL. Refusing to run the H2 export step." >&2
  exit 1
fi

if [[ -z "${DB_URL:-}" || -z "${DB_USERNAME:-}" || ! "${DB_PASSWORD+x}" ]]; then
  echo "Set target MySQL credentials in the current shell: DB_URL, DB_USERNAME, and DB_PASSWORD." >&2
  exit 1
fi

if [[ "${DB_URL,,}" != jdbc:mysql://* ]]; then
  echo "DB_URL must point to MySQL." >&2
  exit 1
fi

export SOURCE_DB_URL="$SOURCE_DB_URL_VALUE"
export SOURCE_DB_USERNAME="${SOURCE_DB_USERNAME_VALUE:-sa}"
export SOURCE_DB_PASSWORD="${SOURCE_DB_PASSWORD_VALUE:-}"
export DB_MIGRATION_OUTPUT_FILE="${DB_MIGRATION_OUTPUT_FILE:-$REPO_ROOT/backups/db-migration/backup.sql}"
export DB_MIGRATION_VALIDATION_FILE="${DB_MIGRATION_VALIDATION_FILE:-$REPO_ROOT/backups/db-migration/validation-report.txt}"

cd "$BACKEND_PATH"
mvn -q -DskipTests exec:java -Dexec.mainClass=com.vaccine.tools.DatabaseMigrationTool
