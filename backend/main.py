# Cola Tracker API v3 — хранение в SQLite вместо data.json.
#
# API полностью совместим с v2 (те же пути и формы ответов), приложение менять не нужно.
# Запуск:  pip install fastapi uvicorn python-multipart
#          python main.py
#
# Переменные окружения:
#   COLA_DB          путь к файлу базы        (по умолчанию cola.db)
#   COLA_PHOTOS      папка для фото           (по умолчанию photos)
#   COLA_AUTH_TOKEN  токен для новой базы     (если не задан — генерируется и печатается)

import hmac
import os
import secrets
import sqlite3
from contextlib import asynccontextmanager, contextmanager
from datetime import date, datetime
from typing import List, Optional

from fastapi import FastAPI, File, HTTPException, Security, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field

DB_PATH = os.environ.get("COLA_DB", "cola.db")
PHOTOS_DIR = os.environ.get("COLA_PHOTOS", "photos")

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
MAX_PHOTO_BYTES = 10 * 1024 * 1024
MAX_DRINK_ML = 5000  # то же ограничение, что в диалоге приложения

SCHEMA = """
CREATE TABLE IF NOT EXISTS meta (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS children (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL,
    photo_url     TEXT,
    monthly_limit INTEGER NOT NULL CHECK (monthly_limit >= 0),
    -- накопленный баланс («кола-метр»): не сгорает, может быть отрицательным
    remaining     INTEGER NOT NULL DEFAULT 0
);
-- AUTOINCREMENT: id записи никогда не переиспользуется, даже после удаления
CREATE TABLE IF NOT EXISTS drinks (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    child_id  INTEGER NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    amount_ml INTEGER NOT NULL,
    timestamp TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_drinks_child_ts ON drinks(child_id, timestamp);
"""


# ===== БАЗА ДАННЫХ =====

def _now() -> datetime:
    """Отдельная функция, чтобы время можно было подменить в тестах."""
    return datetime.now()


def connect() -> sqlite3.Connection:
    # isolation_level=None: транзакциями управляем сами (BEGIN IMMEDIATE ниже)
    conn = sqlite3.connect(DB_PATH, timeout=10, isolation_level=None)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


@contextmanager
def db(write: bool = False):
    """
    Соединение на один запрос. write=True открывает транзакцию сразу с блокировкой
    записи (BEGIN IMMEDIATE): параллельные запросы выстраиваются в очередь,
    и «прочитал-изменил-записал» больше не теряет чужие изменения.
    Любое исключение (в т.ч. HTTPException) откатывает транзакцию.
    """
    conn = connect()
    try:
        if write:
            conn.execute("BEGIN IMMEDIATE")
        yield conn
        if write:
            conn.execute("COMMIT")
    except BaseException:
        if write and conn.in_transaction:
            conn.execute("ROLLBACK")
        raise
    finally:
        conn.close()


def init_db() -> None:
    """Создаёт таблицы и служебные значения. Безопасно вызывать повторно."""
    conn = connect()
    try:
        conn.execute("PRAGMA journal_mode = WAL")
        conn.executescript(SCHEMA)
        conn.execute("BEGIN IMMEDIATE")
        has_token = conn.execute("SELECT 1 FROM meta WHERE key = 'auth_token'").fetchone()
        if not has_token:
            token = os.environ.get("COLA_AUTH_TOKEN")
            if not token:
                token = secrets.token_urlsafe(48)
                print(f"[INFO] Сгенерирован токен авторизации (сохраните его в local.properties): {token}")
            conn.execute("INSERT INTO meta(key, value) VALUES ('auth_token', ?)", (token,))
        conn.execute(
            "INSERT OR IGNORE INTO meta(key, value) VALUES ('last_reset_date', ?)",
            (_now().date().isoformat(),),
        )
        conn.execute("COMMIT")
    finally:
        conn.close()


def apply_monthly_topup(conn: sqlite3.Connection, today: date) -> None:
    """
    Пополняет баланс на monthly_limit за каждый начавшийся месяц.
    Расход месяца отдельно не хранится (считается по истории), поэтому обнулять нечего.
    Если сервер не запрашивали несколько месяцев, пополнение начисляется за все.
    """
    row = conn.execute("SELECT value FROM meta WHERE key = 'last_reset_date'").fetchone()
    last = date.fromisoformat(row["value"])
    months = (today.year - last.year) * 12 + (today.month - last.month)
    if months > 0:
        conn.execute("UPDATE children SET remaining = remaining + monthly_limit * ?", (months,))
        conn.execute(
            "UPDATE meta SET value = ? WHERE key = 'last_reset_date'", (today.isoformat(),)
        )
        print(f"[INFO] Баланс пополнен за {months} мес.")


def _month_bounds(today: date) -> tuple[str, str]:
    start = today.replace(day=1)
    end = date(start.year + (start.month == 12), start.month % 12 + 1, 1)
    return start.isoformat(), end.isoformat()


def fetch_children(conn: sqlite3.Connection, today: date, child_id: Optional[int] = None) -> list:
    """
    consumed_this_month считается суммой записей текущего месяца: так он не может
    разойтись с историей (раньше это был отдельный счётчик, который «плыл»,
    например при удалении записи за прошлый месяц).
    """
    start, end = _month_bounds(today)
    sql = """
        SELECT c.id, c.name, c.photo_url, c.monthly_limit, c.remaining,
               COALESCE((SELECT SUM(d.amount_ml) FROM drinks d
                          WHERE d.child_id = c.id AND d.timestamp >= ? AND d.timestamp < ?), 0)
                   AS consumed_this_month
          FROM children c
    """
    params: list = [start, end]
    if child_id is not None:
        sql += " WHERE c.id = ?"
        params.append(child_id)
    sql += " ORDER BY c.id"
    return [dict(r) for r in conn.execute(sql, params).fetchall()]


# ===== МОДЕЛИ =====

class DrinkRequest(BaseModel):
    amount_ml: int = Field(gt=0, le=MAX_DRINK_ML)  # мл


class ChildResponse(BaseModel):
    id: int
    name: str
    photo_url: Optional[str] = None
    monthly_limit: int
    consumed_this_month: int
    remaining: int


class DrinkHistoryItem(BaseModel):
    id: int
    child_id: int
    amount_ml: int
    timestamp: str


# ===== ПРИЛОЖЕНИЕ =====

@asynccontextmanager
async def lifespan(_app: FastAPI):
    os.makedirs(PHOTOS_DIR, exist_ok=True)
    init_db()
    yield


app = FastAPI(title="Cola Tracker API", version="3.0", lifespan=lifespan)
security = HTTPBearer()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def verify_token(credentials: HTTPAuthorizationCredentials = Security(security)) -> bool:
    with db() as conn:
        row = conn.execute("SELECT value FROM meta WHERE key = 'auth_token'").fetchone()
    # compare_digest: сравнение за постоянное время; bytes — чтобы не падать на не-ASCII
    if not row or not hmac.compare_digest(
        credentials.credentials.encode(), row["value"].encode()
    ):
        raise HTTPException(status_code=401, detail="Неверный токен авторизации")
    return True


def _child_not_found(child_id: int) -> HTTPException:
    return HTTPException(status_code=404, detail=f"Ребёнок с ID {child_id} не найден")


# ===== ЭНДПОИНТЫ =====

@app.get("/")
def read_root():
    return {"message": "Cola Tracker API работает!", "version": "3.0"}


@app.get("/children", response_model=List[ChildResponse])
def get_children(authorized: bool = Security(verify_token)):
    today = _now().date()
    with db(write=True) as conn:  # write: здесь может начислиться месячное пополнение
        apply_monthly_topup(conn, today)
        return fetch_children(conn, today)


@app.post("/children/{child_id}/drink")
def add_drink(child_id: int, drink: DrinkRequest, authorized: bool = Security(verify_token)):
    now = _now()
    with db(write=True) as conn:
        apply_monthly_topup(conn, now.date())
        if not conn.execute("SELECT 1 FROM children WHERE id = ?", (child_id,)).fetchone():
            raise _child_not_found(child_id)

        cur = conn.execute(
            "INSERT INTO drinks(child_id, amount_ml, timestamp) VALUES (?, ?, ?)",
            (child_id, drink.amount_ml, now.isoformat()),
        )
        conn.execute(
            "UPDATE children SET remaining = remaining - ? WHERE id = ?",
            (drink.amount_ml, child_id),
        )
        new_drink = dict(
            conn.execute("SELECT * FROM drinks WHERE id = ?", (cur.lastrowid,)).fetchone()
        )
        child = fetch_children(conn, now.date(), child_id)[0]
    return {"message": "Запись добавлена", "child": child, "drink": new_drink}


@app.get("/children/{child_id}/history", response_model=List[DrinkHistoryItem])
def get_history(child_id: int, authorized: bool = Security(verify_token)):
    with db() as conn:
        rows = conn.execute(
            "SELECT id, child_id, amount_ml, timestamp FROM drinks "
            "WHERE child_id = ? ORDER BY timestamp DESC, id DESC",
            (child_id,),
        ).fetchall()
    return [dict(r) for r in rows]


@app.delete("/drinks/{drink_id}")
def delete_drink(drink_id: int, authorized: bool = Security(verify_token)):
    with db(write=True) as conn:
        apply_monthly_topup(conn, _now().date())
        row = conn.execute("SELECT * FROM drinks WHERE id = ?", (drink_id,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail=f"Запись с ID {drink_id} не найдена")
        deleted = dict(row)
        # Возвращаем в баланс, даже если запись за прошлый месяц: баланс накопительный
        conn.execute(
            "UPDATE children SET remaining = remaining + ? WHERE id = ?",
            (deleted["amount_ml"], deleted["child_id"]),
        )
        conn.execute("DELETE FROM drinks WHERE id = ?", (drink_id,))
    return {"message": "Запись удалена", "deleted_drink": deleted}


@app.post("/children/{child_id}/photo")
async def upload_photo(
    child_id: int,
    file: UploadFile = File(...),
    authorized: bool = Security(verify_token),
):
    with db() as conn:
        if not conn.execute("SELECT 1 FROM children WHERE id = ?", (child_id,)).fetchone():
            raise _child_not_found(child_id)

    extension = os.path.splitext(file.filename or "")[1].lower()
    if extension not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"Неподдерживаемый формат файла. Разрешены: {', '.join(sorted(ALLOWED_EXTENSIONS))}",
        )

    contents = await file.read(MAX_PHOTO_BYTES + 1)
    if len(contents) > MAX_PHOTO_BYTES:
        raise HTTPException(status_code=413, detail="Файл слишком большой (максимум 10 МБ)")

    filename = f"child_{child_id}{extension}"
    try:
        os.makedirs(PHOTOS_DIR, exist_ok=True)
        with open(os.path.join(PHOTOS_DIR, filename), "wb") as f:
            f.write(contents)
    except OSError as e:
        print(f"[ERROR] Ошибка при сохранении файла: {e}")
        raise HTTPException(status_code=500, detail="Ошибка при сохранении фотографии")

    photo_url = f"photos/{filename}"
    with db(write=True) as conn:
        conn.execute("UPDATE children SET photo_url = ? WHERE id = ?", (photo_url, child_id))
    return {"message": "Фотография загружена", "photo_url": photo_url, "child_id": child_id}


@app.get("/photos/{filename}")
def get_photo(filename: str):
    """Фото отдаются без токена (их грузит Coil по обычному URL)."""
    base = os.path.realpath(PHOTOS_DIR)
    path = os.path.realpath(os.path.join(base, filename))
    # commonpath, а не startswith: '/photos_evil' тоже начинается с '/photos'
    if os.path.commonpath([base, path]) != base:
        raise HTTPException(status_code=403, detail="Доступ запрещён")
    if not os.path.isfile(path):
        raise HTTPException(status_code=404, detail=f"Файл '{filename}' не найден")
    return FileResponse(path)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
