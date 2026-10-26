#!/usr/bin/env sh
# Zaxiradan tiklash:  ./restore.sh backups/maktab_2026-10-26_0230.sql.gz
# Backend to'xtatiladi, baza zaxiradagi holatga qaytariladi, backend qayta ishga tushadi.
set -eu

cd "$(dirname "$0")"
[ $# -eq 1 ] && [ -f "$1" ] || { echo "Foydalanish: $0 backups/<fayl>.sql.gz"; exit 1; }
. ./.env 2>/dev/null || true
DB_NAME="${DB_NAME:-maktab_db}"
DB_USERNAME="${DB_USERNAME:-maktab}"

docker compose stop backend
gunzip -c "$1" | docker compose exec -T db psql -q -v ON_ERROR_STOP=1 -U "$DB_USERNAME" -d "$DB_NAME"
docker compose start backend
echo "Tiklandi: $1"
