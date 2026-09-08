#!/bin/bash
# backup_db.sh — Automated PostgreSQL backup with retention policy.
#
# Usage:
#   ./scripts/backup_db.sh              # daily backup
#   ./scripts/backup_db.sh --weekly     # weekly backup (longer retention)
#
# Cron setup (on the production server):
#   # Daily at 2:00 AM
#   0 2 * * * cd /opt/waselni && ./scripts/backup_db.sh
#   # Weekly on Sunday at 3:00 AM
#   0 3 * * 0 cd /opt/waselni && ./scripts/backup_db.sh --weekly
#
# Retention:
#   Daily backups: kept for 7 days
#   Weekly backups: kept for 4 weeks
#
# Restore:
#   docker compose -f docker-compose.prod.yml exec db \
#     psql -U waselni -d waslni < /backups/daily_2026-09-08.sql.gz

set -euo pipefail

WEEKLY="${1:-}"
DAILY_RETENTION_DAYS=7
WEEKLY_RETENTION_WEEKS=4
BACKUP_DIR="/opt/waselni/backups"
DB_USER="${POSTGRES_USER:-waselni}"
DB_NAME="${POSTGRES_DB:-waslni}"
TIMESTAMP=$(date +%Y-%m-%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Determine prefix + retention
if [ "$WEEKLY" = "--weekly" ]; then
    PREFIX="weekly"
    RETENTION_DAYS=$((WEEKLY_RETENTION_WEEKS * 7))
else
    PREFIX="daily"
    RETENTION_DAYS=$DAILY_RETENTION_DAYS
fi

FILENAME="${PREFIX}_${TIMESTAMP}.sql.gz"
FILEPATH="${BACKUP_DIR}/${FILENAME}"

echo "[$(date)] Starting ${PREFIX} backup → $FILENAME"

# Run pg_dump inside the Docker container, pipe through gzip
docker compose -f /opt/waselni/docker-compose.prod.yml exec -T db \
    pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --no-acl \
    | gzip > "$FILEPATH"

# Verify the backup is not empty
if [ ! -s "$FILEPATH" ]; then
    echo "[$(date)] ERROR: Backup file is empty!"
    rm -f "$FILEPATH"
    exit 1
fi

SIZE=$(du -h "$FILEPATH" | cut -f1)
echo "[$(date)] Backup completed: $FILENAME ($SIZE)"

# Clean up old backups
echo "[$(date)] Cleaning up ${PREFIX} backups older than ${RETENTION_DAYS} days..."
find "$BACKUP_DIR" -name "${PREFIX}_*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete
echo "[$(date)] Cleanup complete."

# List remaining backups
echo "[$(date)] Current ${PREFIX} backups:"
ls -lh "$BACKUP_DIR"/${PREFIX}_*.sql.gz 2>/dev/null || echo "  (none)"

echo "[$(date)] Done."
