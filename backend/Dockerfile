FROM python:3.12-slim

# Версии как на текущем сервере (проверено рабочими)
COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

# Часовой пояс важен: метки времени пишутся «наивными» (без смещения), приложение читает их
# как местное время, а границы месяца считаются по ним. В UTC записи съехали бы на 3 часа.
RUN apt-get update && apt-get install -y --no-install-recommends tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/Moscow \
    PYTHONUNBUFFERED=1 \
    COLA_DB=/data/cola.db \
    COLA_PHOTOS=/data/photos

# Не root: данные лежат в томе /data
RUN useradd --uid 10001 --no-create-home --shell /usr/sbin/nologin cola \
    && mkdir /data && chown cola /data
USER cola

WORKDIR /app
COPY main.py manage.py ./

VOLUME /data
EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/', timeout=3)"

# Один процесс: SQLite + WAL с BEGIN IMMEDIATE выдерживает нагрузку этого приложения с запасом
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers"]
