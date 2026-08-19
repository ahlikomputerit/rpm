from __future__ import annotations

import json
import os
import sqlite3
import threading
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


DATA_DIR = Path(os.getenv("AGENT_DATA_DIR", Path(__file__).resolve().parents[1] / "data"))
DATA_DIR.mkdir(parents=True, exist_ok=True)
DB_PATH = Path(os.getenv("AGENT_DB_PATH", DATA_DIR / "agent.sqlite3"))

_local = threading.local()


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def conn() -> sqlite3.Connection:
    connection = getattr(_local, "connection", None)
    if connection is None:
        connection = sqlite3.connect(DB_PATH, check_same_thread=False)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA journal_mode=WAL")
        connection.execute("PRAGMA foreign_keys=ON")
        connection.execute("PRAGMA busy_timeout=5000")
        _local.connection = connection
    return connection


def init_db() -> None:
    connection = conn()
    connection.executescript(
        """
        CREATE TABLE IF NOT EXISTS tasks (
            id TEXT PRIMARY KEY,
            instruction TEXT NOT NULL,
            mode TEXT NOT NULL DEFAULT 'manual_approval',
            status TEXT NOT NULL DEFAULT 'queued',
            start_url TEXT,
            allowed_domains TEXT NOT NULL DEFAULT '[]',
            plan_json TEXT NOT NULL DEFAULT '[]',
            cursor INTEGER NOT NULL DEFAULT 0,
            result TEXT,
            error TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL,
            finished_at TEXT
        );

        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            task_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
            seq INTEGER NOT NULL,
            kind TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            created_at TEXT NOT NULL,
            UNIQUE(task_id, seq)
        );

        CREATE TABLE IF NOT EXISTS approvals (
            id TEXT PRIMARY KEY,
            task_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
            action_index INTEGER NOT NULL,
            action_hash TEXT NOT NULL,
            summary TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'pending',
            created_at TEXT NOT NULL,
            decided_at TEXT,
            UNIQUE(task_id, action_index, status)
        );

        CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
        CREATE INDEX IF NOT EXISTS idx_events_task ON events(task_id, seq);
        CREATE INDEX IF NOT EXISTS idx_approvals_status ON approvals(status);
        """
    )
    connection.commit()


def _decode(row: sqlite3.Row | None) -> dict[str, Any] | None:
    if row is None:
        return None
    item = dict(row)
    for field in ("allowed_domains", "plan_json"):
        try:
            item[field] = json.loads(item[field])
        except (TypeError, json.JSONDecodeError):
            item[field] = []
    return item


def create_task(instruction: str, mode: str, start_url: str | None, allowed_domains: list[str]) -> dict[str, Any]:
    task_id = str(uuid.uuid4())
    timestamp = now_iso()
    connection = conn()
    connection.execute(
        "INSERT INTO tasks (id, instruction, mode, start_url, allowed_domains, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
        (task_id, instruction, mode, start_url, json.dumps(allowed_domains), timestamp, timestamp),
    )
    connection.commit()
    return get_task(task_id)  # type: ignore[return-value]


def get_task(task_id: str) -> dict[str, Any] | None:
    return _decode(conn().execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone())


def list_tasks(limit: int = 50) -> list[dict[str, Any]]:
    rows = conn().execute("SELECT * FROM tasks ORDER BY created_at DESC LIMIT ?", (limit,)).fetchall()
    return [_decode(row) for row in rows if row is not None]  # type: ignore[list-item]


def update_task(task_id: str, **fields: Any) -> dict[str, Any] | None:
    if not fields:
        return get_task(task_id)
    fields["updated_at"] = now_iso()
    encoded = {"allowed_domains", "plan_json"}
    values = []
    assignments = []
    for key, value in fields.items():
        if key in encoded:
            value = json.dumps(value)
        assignments.append(f"{key} = ?")
        values.append(value)
    values.append(task_id)
    conn().execute(f"UPDATE tasks SET {', '.join(assignments)} WHERE id = ?", values)
    conn().commit()
    return get_task(task_id)


def append_event(task_id: str, kind: str, payload: dict[str, Any]) -> dict[str, Any]:
    connection = conn()
    row = connection.execute("SELECT COALESCE(MAX(seq), 0) + 1 AS next_seq FROM events WHERE task_id = ?", (task_id,)).fetchone()
    seq = int(row["next_seq"])
    timestamp = now_iso()
    connection.execute(
        "INSERT INTO events (task_id, seq, kind, payload_json, created_at) VALUES (?, ?, ?, ?, ?)",
        (task_id, seq, kind, json.dumps(payload, ensure_ascii=False), timestamp),
    )
    connection.commit()
    return {"task_id": task_id, "seq": seq, "kind": kind, "payload": payload, "created_at": timestamp}


def list_events(task_id: str) -> list[dict[str, Any]]:
    rows = conn().execute("SELECT * FROM events WHERE task_id = ? ORDER BY seq", (task_id,)).fetchall()
    return [
        {"task_id": row["task_id"], "seq": row["seq"], "kind": row["kind"], "payload": json.loads(row["payload_json"]), "created_at": row["created_at"]}
        for row in rows
    ]


def create_approval(task_id: str, action_index: int, action_hash: str, summary: str) -> dict[str, Any]:
    approval_id = str(uuid.uuid4())
    timestamp = now_iso()
    conn().execute(
        "INSERT INTO approvals (id, task_id, action_index, action_hash, summary, created_at) VALUES (?, ?, ?, ?, ?, ?)",
        (approval_id, task_id, action_index, action_hash, summary, timestamp),
    )
    conn().commit()
    return get_approval(approval_id)  # type: ignore[return-value]


def get_approval(approval_id: str) -> dict[str, Any] | None:
    row = conn().execute("SELECT * FROM approvals WHERE id = ?", (approval_id,)).fetchone()
    return dict(row) if row else None


def get_pending_approval(task_id: str) -> dict[str, Any] | None:
    row = conn().execute(
        "SELECT * FROM approvals WHERE task_id = ? AND status = 'pending' ORDER BY created_at DESC LIMIT 1",
        (task_id,),
    ).fetchone()
    return dict(row) if row else None


def get_current_approval(task_id: str) -> dict[str, Any] | None:
    row = conn().execute(
        "SELECT * FROM approvals WHERE task_id = ? ORDER BY created_at DESC LIMIT 1",
        (task_id,),
    ).fetchone()
    return dict(row) if row else None


def decide_approval(approval_id: str, status: str) -> dict[str, Any] | None:
    conn().execute(
        "UPDATE approvals SET status = ?, decided_at = ? WHERE id = ? AND status = 'pending'",
        (status, now_iso(), approval_id),
    )
    conn().commit()
    return get_approval(approval_id)


def fail_active_tasks() -> None:
    conn().execute(
        "UPDATE tasks SET status = 'failed', error = 'Service restarted while task was active', updated_at = ?, finished_at = ? WHERE status IN ('queued', 'planning', 'running', 'waiting_approval', 'paused')",
        (now_iso(), now_iso()),
    )
    conn().commit()
