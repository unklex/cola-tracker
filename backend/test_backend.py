"""
Тесты бэкенда v3. Работают на временной SQLite-базе и не трогают ни реальный сервер,
ни локальные данные:  cd backend && python -m pytest -q
"""
import json
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

import main
import manage

TOKEN = "test-token"
AUTH = {"Authorization": f"Bearer {TOKEN}"}


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setattr(main, "DB_PATH", str(tmp_path / "test.db"))
    monkeypatch.setattr(main, "PHOTOS_DIR", str(tmp_path / "photos"))
    monkeypatch.setenv("COLA_AUTH_TOKEN", TOKEN)
    monkeypatch.setattr(main, "_now", lambda: datetime(2026, 10, 6, 12, 0, 0))
    with TestClient(main.app) as c:
        yield c


def add_child(name="Маша", limit=1000, balance=None):
    manage.add_child(name, limit, balance)


def child(client, child_id=1):
    return next(c for c in client.get("/children", headers=AUTH).json() if c["id"] == child_id)


def test_auth(client):
    # нет заголовка: 401 или 403 в зависимости от версии FastAPI (приложение их не различает)
    assert client.get("/children").status_code in (401, 403)
    assert client.get("/children", headers={"Authorization": "Bearer nope"}).status_code == 401
    assert client.get("/children", headers=AUTH).status_code == 200
    assert client.get("/").status_code == 200  # корень без токена


def test_add_drink_updates_counters(client):
    add_child()
    r = client.post("/children/1/drink", json={"amount_ml": 250}, headers=AUTH)
    assert r.status_code == 200
    body = r.json()
    assert body["child"]["consumed_this_month"] == 250
    assert body["child"]["remaining"] == 750
    assert body["drink"]["amount_ml"] == 250
    assert child(client) == body["child"]


def test_unknown_child_404(client):
    assert client.post("/children/99/drink", json={"amount_ml": 100}, headers=AUTH).status_code == 404


@pytest.mark.parametrize("amount", [0, -5, 5001])
def test_invalid_amount_rejected(client, amount):
    add_child()
    r = client.post("/children/1/drink", json={"amount_ml": amount}, headers=AUTH)
    assert r.status_code == 422
    assert child(client)["remaining"] == 1000  # ничего не записалось


def test_drink_ids_never_reused(client):
    """Регрессия: в v2 id = len(history)+1 дублировался после удаления из середины."""
    add_child()
    ids = [client.post("/children/1/drink", json={"amount_ml": 100}, headers=AUTH).json()["drink"]["id"]
           for _ in range(3)]
    assert ids == [1, 2, 3]
    assert client.delete("/drinks/2", headers=AUTH).status_code == 200
    new_id = client.post("/children/1/drink", json={"amount_ml": 100}, headers=AUTH).json()["drink"]["id"]
    assert new_id == 4
    history_ids = [d["id"] for d in client.get("/children/1/history", headers=AUTH).json()]
    assert sorted(history_ids) == [1, 3, 4]


def test_delete_refunds_balance(client):
    add_child()
    drink_id = client.post("/children/1/drink", json={"amount_ml": 300}, headers=AUTH).json()["drink"]["id"]
    r = client.delete(f"/drinks/{drink_id}", headers=AUTH)
    assert r.json()["deleted_drink"]["amount_ml"] == 300
    c = child(client)
    assert (c["consumed_this_month"], c["remaining"]) == (0, 1000)
    assert client.delete(f"/drinks/{drink_id}", headers=AUTH).status_code == 404


def test_deleting_old_month_drink_does_not_touch_month_usage(client):
    """Регрессия: в v2 удаление записи за прошлый месяц уменьшало расход текущего."""
    add_child()
    client.post("/children/1/drink", json={"amount_ml": 200}, headers=AUTH)
    with main.db(write=True) as conn:
        cur = conn.execute(
            "INSERT INTO drinks(child_id, amount_ml, timestamp) VALUES (1, 500, '2026-09-15T10:00:00')"
        )
        old_id = cur.lastrowid
    assert child(client)["consumed_this_month"] == 200  # сентябрьская запись не считается

    client.delete(f"/drinks/{old_id}", headers=AUTH)
    c = child(client)
    assert c["consumed_this_month"] == 200  # расход месяца не изменился
    assert c["remaining"] == 800 + 500      # а баланс (накопительный) получил возврат


def test_monthly_topup_covers_skipped_months(client):
    add_child(limit=1000, balance=100)
    with main.db(write=True) as conn:
        conn.execute("UPDATE meta SET value = '2026-08-20' WHERE key = 'last_reset_date'")
    assert child(client)["remaining"] == 100 + 2 * 1000  # август -> октябрь = 2 месяца
    assert child(client)["remaining"] == 2100             # повторный запрос не начисляет снова


def test_history_newest_first(client):
    add_child()
    with main.db(write=True) as conn:
        conn.executemany(
            "INSERT INTO drinks(child_id, amount_ml, timestamp) VALUES (1, ?, ?)",
            [(100, "2026-10-01T09:00:00"), (200, "2026-10-03T09:00:00"), (300, "2026-10-02T09:00:00")],
        )
    assert [d["amount_ml"] for d in client.get("/children/1/history", headers=AUTH).json()] == [200, 300, 100]


def test_concurrent_adds_lose_nothing(client):
    """То, ради чего переезжаем с JSON: параллельные запросы не теряют записи."""
    add_child(limit=100000)

    def add(_):
        return client.post("/children/1/drink", json={"amount_ml": 10}, headers=AUTH).status_code

    with ThreadPoolExecutor(max_workers=8) as pool:
        assert set(pool.map(add, range(40))) == {200}
    c = child(client)
    assert c["consumed_this_month"] == 400
    assert c["remaining"] == 100000 - 400
    assert len(client.get("/children/1/history", headers=AUTH).json()) == 40


def test_photo_upload_and_download(client):
    add_child()
    r = client.post("/children/1/photo", headers=AUTH, files={"file": ("a.jpg", b"jpegbytes", "image/jpeg")})
    assert r.status_code == 200
    assert child(client)["photo_url"] == "photos/child_1.jpg"
    assert client.get("/photos/child_1.jpg").content == b"jpegbytes"
    bad = client.post("/children/1/photo", headers=AUTH, files={"file": ("a.exe", b"x", "application/x")})
    assert bad.status_code == 400


def test_photo_path_traversal_blocked(client):
    for name in ["../test.db", "..", "../../etc/passwd"]:
        with pytest.raises(HTTPException) as e:
            main.get_photo(name)
        assert e.value.status_code == 403


def test_import_from_legacy_json(client, tmp_path):
    legacy = {
        "auth_token": "legacy-token",
        "last_reset_date": "2026-10-01",
        "children": [
            {"id": 1, "name": "А", "photo_url": None, "monthly_limit": 1000,
             "consumed_this_month": 999, "remaining": 400},
        ],
        # дубликат id=1 — следствие бага v2
        "drinks_history": [
            {"id": 1, "child_id": 1, "amount_ml": 250, "timestamp": "2026-10-02T10:00:00"},
            {"id": 1, "child_id": 1, "amount_ml": 330, "timestamp": "2026-10-03T10:00:00"},
        ],
    }
    path = tmp_path / "data.json"
    path.write_text(json.dumps(legacy), encoding="utf-8")
    manage.import_json(str(path))

    h = {"Authorization": "Bearer legacy-token"}
    history = client.get("/children/1/history", headers=h).json()
    assert len(history) == 2 and len({d["id"] for d in history}) == 2
    c = client.get("/children", headers=h).json()[0]
    assert c["remaining"] == 400
    assert c["consumed_this_month"] == 580  # по истории, а не устаревшие 999
    with pytest.raises(SystemExit):
        manage.import_json(str(path))  # повторный импорт в непустую базу запрещён
