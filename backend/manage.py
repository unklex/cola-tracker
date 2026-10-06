"""
Управление базой Cola Tracker.

    python manage.py import-json data.json      # перенос данных из старого бэкенда (v2)
    python manage.py add-child "Маша" 1000      # новый ребёнок (стартовый баланс = начисление)
    python manage.py add-child "Петя" 1000 --balance 0
    python manage.py list
"""
import argparse
import json
import sys

import main


def import_json(path: str) -> None:
    with open(path, encoding="utf-8") as f:
        data = json.load(f)

    main.init_db()
    today = main._now().date()

    with main.db(write=True) as conn:
        if conn.execute("SELECT 1 FROM children LIMIT 1").fetchone():
            sys.exit("В базе уже есть дети — импорт только в пустую базу.")

        if data.get("auth_token"):
            conn.execute("UPDATE meta SET value = ? WHERE key = 'auth_token'", (data["auth_token"],))
        if data.get("last_reset_date"):
            conn.execute(
                "UPDATE meta SET value = ? WHERE key = 'last_reset_date'",
                (data["last_reset_date"][:10],),
            )

        for c in data.get("children", []):
            conn.execute(
                "INSERT INTO children(id, name, photo_url, monthly_limit, remaining) "
                "VALUES (?, ?, ?, ?, ?)",
                (c["id"], c["name"], c.get("photo_url"), c["monthly_limit"], c["remaining"]),
            )

        reassigned = 0
        for d in data.get("drinks_history", []):
            # В v2 id считался как len()+1 и мог дублироваться — дубликатам даём новый id
            taken = conn.execute("SELECT 1 FROM drinks WHERE id = ?", (d["id"],)).fetchone()
            if taken:
                reassigned += 1
                conn.execute(
                    "INSERT INTO drinks(child_id, amount_ml, timestamp) VALUES (?, ?, ?)",
                    (d["child_id"], d["amount_ml"], d["timestamp"]),
                )
            else:
                conn.execute(
                    "INSERT INTO drinks(id, child_id, amount_ml, timestamp) VALUES (?, ?, ?, ?)",
                    (d["id"], d["child_id"], d["amount_ml"], d["timestamp"]),
                )

        # Расход месяца теперь считается по истории — предупредим, если старый счётчик с ней не сходился
        derived = {c["id"]: c["consumed_this_month"] for c in main.fetch_children(conn, today)}
        for c in data.get("children", []):
            if c.get("consumed_this_month") != derived[c["id"]]:
                print(
                    f"[WARN] {c['name']}: в data.json расход месяца {c.get('consumed_this_month')} мл, "
                    f"по истории {derived[c['id']]} мл — используется значение по истории"
                )

    print(
        f"Импортировано: детей {len(data.get('children', []))}, "
        f"записей {len(data.get('drinks_history', []))} (новый id у {reassigned} дубликатов)"
    )


def add_child(name: str, limit: int, balance) -> None:
    main.init_db()
    with main.db(write=True) as conn:
        cur = conn.execute(
            "INSERT INTO children(name, monthly_limit, remaining) VALUES (?, ?, ?)",
            (name, limit, limit if balance is None else balance),
        )
    print(f"Добавлен ребёнок id={cur.lastrowid}")


def list_children() -> None:
    main.init_db()
    with main.db() as conn:
        for c in main.fetch_children(conn, main._now().date()):
            print(
                f"{c['id']}: {c['name']}  начисление {c['monthly_limit']}  "
                f"выпито {c['consumed_this_month']}  баланс {c['remaining']}"
            )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("import-json")
    p.add_argument("path")
    p = sub.add_parser("add-child")
    p.add_argument("name")
    p.add_argument("limit", type=int)
    p.add_argument("--balance", type=int, default=None)
    sub.add_parser("list")

    args = parser.parse_args()
    if args.cmd == "import-json":
        import_json(args.path)
    elif args.cmd == "add-child":
        add_child(args.name, args.limit, args.balance)
    else:
        list_children()
