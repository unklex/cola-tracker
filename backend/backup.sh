#!/bin/bash
# Ночной бэкап Cola Tracker (контейнер colatracker): база SQLite + фото.
# Установка на сервере:
#   install -m 755 backup.sh /usr/local/bin/colatracker-backup.sh
#   echo '30 3 * * * root /usr/local/bin/colatracker-backup.sh >> /var/log/colatracker-backup.log 2>&1' \
#     > /etc/cron.d/colatracker-backup
#
# База копируется через SQLite backup API (согласованный снимок живой базы), а не cp файла:
# при включённом WAL простое копирование может дать битую базу.
# Бэкапы лежат на том же сервере — от потери самого сервера они не защищают,
# периодически забирайте каталог $DEST к себе (scp/rsync).
set -euo pipefail

CONTAINER="${COLA_CONTAINER:-colatracker}"
DEST="${COLA_BACKUP_DIR:-/var/backups/colatracker}"
KEEP_DAYS="${COLA_BACKUP_KEEP_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"

umask 077
mkdir -p "$DEST"

docker exec "$CONTAINER" python -c "
import sqlite3
src = sqlite3.connect('/data/cola.db')
dst = sqlite3.connect('/tmp/backup.db')
src.backup(dst)
dst.close()
"
docker cp "$CONTAINER:/tmp/backup.db" "$DEST/cola-$STAMP.db"
docker exec "$CONTAINER" rm -f /tmp/backup.db

# Не оставляем «успешный» бэкап, который нельзя открыть
CHECK="$(sqlite3 "$DEST/cola-$STAMP.db" 'PRAGMA integrity_check;')"
if [ "$CHECK" != "ok" ]; then
    echo "[$(date -Is)] ОШИБКА: integrity_check = $CHECK" >&2
    rm -f "$DEST/cola-$STAMP.db"
    exit 1
fi
CHILDREN="$(sqlite3 "$DEST/cola-$STAMP.db" 'SELECT COUNT(*) FROM children;')"
DRINKS="$(sqlite3 "$DEST/cola-$STAMP.db" 'SELECT COUNT(*) FROM drinks;')"
gzip -f "$DEST/cola-$STAMP.db"

docker cp "$CONTAINER:/data/photos" - | gzip > "$DEST/photos-$STAMP.tar.gz"

find "$DEST" -type f \( -name 'cola-*.db.gz' -o -name 'photos-*.tar.gz' \) -mtime +"$KEEP_DAYS" -delete

echo "[$(date -Is)] OK: cola-$STAMP.db.gz (детей: $CHILDREN, записей: $DRINKS), photos-$STAMP.tar.gz; хранится $KEEP_DAYS дн."
