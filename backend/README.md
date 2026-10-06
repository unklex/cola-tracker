# Cola Tracker backend v3 (SQLite)

API совместим с v2 — приложение менять не нужно. Тесты: `python -m pytest -q` (временная база).

## Переезд с v2 (data.json) на Docker

На сервере, из этой папки. Сначала сделайте копию `data.json` — миграция её не меняет, но бэкап не лишний.

```bash
docker compose build

# 1. импорт старых данных в том (токен переносится из data.json)
docker compose run --rm \
  -v /root/Projects/ColaTrackerBack/data.json:/import/data.json:ro \
  -v /root/Projects/ColaTrackerBack/photos:/import/photos:ro \
  colatracker python manage.py import-json /import/data.json

# 2. фото: скопировать в том
docker compose run --rm --no-deps -v /root/Projects/ColaTrackerBack/photos:/import/photos:ro \
  colatracker sh -c 'mkdir -p /data/photos && cp -p /import/photos/* /data/photos/'

# 3. проверить, что цифры совпадают с приложением
docker compose run --rm colatracker python manage.py list

# 4. переключить: остановить старый сервис и поднять контейнер (порт 8000 тот же)
systemctl stop colatracker && systemctl disable colatracker
docker compose up -d
curl -s http://127.0.0.1:8000/
```

Откат: `docker compose down && systemctl enable --now colatracker` — старый `data.json` не тронут.

## Бэкап

```bash
docker compose exec colatracker python -c "import sqlite3; s=sqlite3.connect('/data/cola.db'); d=sqlite3.connect('/data/backup.db'); s.backup(d)"
```
(или копируйте том целиком; для регулярного бэкапа удобнее cron + `docker cp`).

## Управление

```bash
docker compose exec colatracker python manage.py add-child "Имя" 1250
docker compose exec colatracker python manage.py list
```
