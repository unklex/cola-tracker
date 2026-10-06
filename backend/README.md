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

## HTTPS

API публикуется через nginx + Let's Encrypt на `https://cola.st77.ru` (конфиг — `nginx-cola.conf`, сертификат
продлевается certbot автоматически). Контейнер слушает порт 8000; после перехода всех клиентов на HTTPS
порт в `docker-compose.yml` стоит сузить до `127.0.0.1:8000:8000` и закрыть 8000 в файрволе.

## Бэкап

`backup.sh` делает согласованный снимок базы (SQLite backup API) и архив фото, проверяет
`integrity_check`, хранит 14 дней. На сервере уже стоит в cron (каждую ночь в 03:30):

```bash
install -m 755 backup.sh /usr/local/bin/colatracker-backup.sh
echo '30 3 * * * root /usr/local/bin/colatracker-backup.sh >> /var/log/colatracker-backup.log 2>&1' > /etc/cron.d/colatracker-backup
```

Файлы: `/var/backups/colatracker/cola-*.db.gz`, `photos-*.tar.gz`. Они лежат на том же сервере —
периодически забирайте каталог к себе (`scp -r root@сервер:/var/backups/colatracker .`).

Восстановление (контейнер остановить, затем подменить базу):

```bash
docker compose stop
gunzip -c /var/backups/colatracker/cola-ГГГГММДД-ЧЧММСС.db.gz > /tmp/cola.db
docker run --rm -v colatracker_cola-data:/data -v /tmp/cola.db:/restore.db:ro python:3.12-slim   sh -c 'cp /restore.db /data/cola.db && rm -f /data/cola.db-wal /data/cola.db-shm && mkdir -p /data/photos && chown -R 10001 /data'
docker compose start
```

## Идемпотентность добавления

`POST /children/{id}/drink` принимает необязательный `request_id` (UUID от приложения). Повтор с тем же
ключом возвращает ту же запись и ничего не списывает второй раз — так «Повторить» в приложении безопасен
после обрыва связи. Тот же ключ для другой суммы или другого ребёнка — `409`. Колонка `drinks.request_id`
добавляется в существующую базу автоматически при старте (`migrate()` в `main.py`), данные не меняются.

## Управление

```bash
docker compose exec colatracker python manage.py add-child "Имя" 1250
docker compose exec colatracker python manage.py list
```
