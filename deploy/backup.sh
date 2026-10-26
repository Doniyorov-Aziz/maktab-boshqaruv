#!/usr/bin/env sh
# Kunlik PostgreSQL zaxirasi: deploy/backups/maktab_YYYY-MM-DD_HHMM.sql.gz, 7 kundan eskilari o'chiriladi.
# Cron (har kuni 02:30, Toshkent vaqti serverda sozlangan bo'lsa):
#   30 2 * * * /home/ubuntu/maktab-boshqaruv/deploy/backup.sh >> /home/ubuntu/maktab-backup.log 2>&1
set -eu

cd "$(dirname "$0")"
mkdir -p backups
. ./.env 2>/dev/null || true
DB_NAME="${DB_NAME:-maktab_db}"
DB_USERNAME="${DB_USERNAME:-maktab}"

file="backups/maktab_$(date +%F_%H%M).sql.gz"
docker compose exec -T db pg_dump -U "$DB_USERNAME" -d "$DB_NAME" --no-owner --clean --if-exists | gzip > "$file.tmp"
mv "$file.tmp" "$file"
find backups -name 'maktab_*.sql.gz' -mtime +7 -delete
echo "$(date '+%F %T') zaxira tayyor: $file ($(du -h "$file" | cut -f1))"
