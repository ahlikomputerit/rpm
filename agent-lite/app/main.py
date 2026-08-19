from __future__ import annotations

from contextlib import asynccontextmanager
from pathlib import Path
from urllib.parse import urlparse

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from . import db
from .config import settings
from .runner import TaskRunner
from .schemas import ApprovalDecision, TaskCreate


runner = TaskRunner()
STATIC_DIR = Path(__file__).resolve().parents[1] / "static"


@asynccontextmanager
async def lifespan(_: FastAPI):
    await runner.start()
    yield
    await runner.stop()


app = FastAPI(title=settings.app_name, version="0.1.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://127.0.0.1:8787", "http://localhost:8787"],
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


@app.get("/", include_in_schema=False)
async def index() -> FileResponse:
    return FileResponse(STATIC_DIR / "index.html")


@app.get("/healthz")
async def healthz() -> dict[str, str]:
    return {"status": "ok", "service": settings.app_name}


@app.get("/readyz")
async def readyz() -> dict[str, str | int]:
    db.init_db()
    return {"status": "ready", "queue_size": runner.queue.qsize()}


@app.post("/api/tasks", status_code=201)
async def create_task(payload: TaskCreate) -> dict:
    domains = list(payload.allowed_domains)
    if payload.start_url and not domains:
        host = urlparse(payload.start_url).hostname
        if host:
            domains = [host.lower()]
    task = db.create_task(payload.instruction, payload.mode, payload.start_url, domains)
    db.append_event(task["id"], "created", {"instruction": payload.instruction, "mode": payload.mode, "allowed_domains": domains})
    await runner.enqueue(task["id"])
    return db.get_task(task["id"]) or task


@app.get("/api/tasks")
async def list_tasks() -> list[dict]:
    return db.list_tasks()


@app.get("/api/tasks/{task_id}")
async def get_task(task_id: str) -> dict:
    task = db.get_task(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"task": task, "events": db.list_events(task_id), "approval": db.get_pending_approval(task_id)}


@app.get("/api/tasks/{task_id}/artifacts/{artifact_path:path}")
async def get_artifact(task_id: str, artifact_path: str) -> FileResponse:
    task = db.get_task(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    root = (db.DATA_DIR / "workspaces" / task_id).resolve()
    candidate = (root / artifact_path).resolve()
    if root not in candidate.parents or not candidate.is_file():
        raise HTTPException(status_code=404, detail="Artifact not found")
    return FileResponse(candidate)


@app.post("/api/tasks/{task_id}/cancel")
async def cancel_task(task_id: str) -> dict:
    task = await runner.cancel(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task


@app.get("/api/approvals/pending")
async def pending_approvals() -> list[dict]:
    approvals = []
    for task in db.list_tasks():
        approval = db.get_pending_approval(task["id"])
        if approval:
            approvals.append({"task": task, "approval": approval})
    return approvals


@app.post("/api/approvals/{approval_id}/decision")
async def decide_approval(approval_id: str, payload: ApprovalDecision) -> dict:
    result = await runner.approve(approval_id, payload.decision == "approved")
    if not result:
        raise HTTPException(status_code=404, detail="Approval not found")
    return result


@app.post("/api/emergency-stop")
async def emergency_stop() -> dict[str, int]:
    count = 0
    for task in db.list_tasks():
        if task["status"] in {"queued", "planning", "running", "waiting_approval", "paused"}:
            await runner.cancel(task["id"], "Emergency stop")
            count += 1
    return {"cancelled": count}
