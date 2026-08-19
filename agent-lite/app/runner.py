from __future__ import annotations

import asyncio
import json
from pathlib import Path
from typing import Any

from . import db
from .browser_worker import BrowserWorker
from .config import settings
from .model import plan_task
from .policy import action_hash, approval_summary, requires_approval
from .resource_guard import ensure_browser_capacity
from .schemas import AgentPlan, Action


class TaskRunner:
    def __init__(self) -> None:
        self.queue: asyncio.Queue[str] = asyncio.Queue(maxsize=20)
        self.sessions: dict[str, BrowserWorker] = {}
        self.worker_task: asyncio.Task[None] | None = None
        self.stop_requested = False

    async def start(self) -> None:
        db.init_db()
        db.fail_active_tasks()
        self.stop_requested = False
        self.worker_task = asyncio.create_task(self._loop())

    async def stop(self) -> None:
        self.stop_requested = True
        if self.worker_task:
            self.worker_task.cancel()
            try:
                await self.worker_task
            except asyncio.CancelledError:
                pass
        for worker in list(self.sessions.values()):
            await worker.close()
        self.sessions.clear()

    async def enqueue(self, task_id: str) -> None:
        try:
            self.queue.put_nowait(task_id)
        except asyncio.QueueFull:
            db.update_task(task_id, status="failed", error="Task queue is full", finished_at=db.now_iso())
            db.append_event(task_id, "error", {"message": "Task queue is full"})

    async def _loop(self) -> None:
        while not self.stop_requested:
            task_id = await self.queue.get()
            try:
                await asyncio.wait_for(self.run_task(task_id), timeout=settings.task_timeout_seconds)
            except Exception as exc:  # the task must fail safely, not kill the loop
                task = db.get_task(task_id)
                if task and task["status"] not in {"succeeded", "cancelled", "failed"}:
                    db.update_task(task_id, status="failed", error=str(exc), finished_at=db.now_iso())
                    db.append_event(task_id, "error", {"message": str(exc)})
                worker = self.sessions.pop(task_id, None)
                if worker:
                    await worker.close()
            finally:
                self.queue.task_done()

    async def run_task(self, task_id: str) -> None:
        task = db.get_task(task_id)
        if not task or task["status"] in {"cancelled", "failed", "succeeded"}:
            return

        pending = db.get_current_approval(task_id)
        if pending:
            if pending["status"] == "rejected":
                db.update_task(task_id, status="cancelled", error="Approval rejected", finished_at=db.now_iso())
                return
            if pending["status"] not in {"approved", "pending"}:
                return

        db.update_task(task_id, status="planning")
        db.append_event(task_id, "status", {"status": "planning"})

        plan = AgentPlan.model_validate({"summary": "", "actions": task["plan_json"]})
        if not plan.actions:
            plan = plan_task(task["instruction"], task["start_url"])
            if len(plan.actions) > settings.max_steps:
                plan = AgentPlan(summary=plan.summary, actions=plan.actions[: settings.max_steps])
            db.update_task(task_id, plan_json=[action.model_dump() for action in plan.actions])
            db.append_event(task_id, "plan", {"summary": plan.summary, "actions": [action.model_dump() for action in plan.actions]})

        worker = self.sessions.get(task_id)
        if not worker:
            ensure_browser_capacity()
            workspace = Path(db.DATA_DIR) / "workspaces" / task_id
            worker = BrowserWorker(task_id, workspace, task["allowed_domains"])
            await worker.start()
            self.sessions[task_id] = worker

        db.update_task(task_id, status="running")
        db.append_event(task_id, "status", {"status": "running"})

        cursor = int(task.get("cursor") or 0)
        while cursor < len(plan.actions):
            current = db.get_task(task_id)
            if not current or current["status"] in {"cancelled", "failed"}:
                await self._cleanup(task_id)
                return

            action = plan.actions[cursor]
            needs_approval, policy_reason = requires_approval(action)
            pending = db.get_current_approval(task_id)
            if needs_approval and not (pending and pending["action_index"] == cursor and pending["status"] == "approved"):
                approval = pending if pending and pending["action_index"] == cursor else db.create_approval(task_id, cursor, action_hash(action), approval_summary(action))
                db.update_task(task_id, status="waiting_approval")
                db.append_event(task_id, "approval_required", {"approval_id": approval["id"], "action_index": cursor, "summary": approval["summary"], "policy_reason": policy_reason})
                return

            if action.tool == "task.complete":
                db.update_task(task_id, status="succeeded", result=str(action.args.get("message", "Task selesai")), cursor=cursor + 1, finished_at=db.now_iso())
                db.append_event(task_id, "completed", {"message": action.args.get("message", "Task selesai")})
                await self._cleanup(task_id)
                return

            db.append_event(task_id, "action", {"index": cursor, "tool": action.tool, "args": action.args, "reason": action.reason})
            result = await worker.execute(action.tool, action.args)
            db.append_event(task_id, "result", {"index": cursor, "tool": action.tool, "result": result})
            cursor += 1
            db.update_task(task_id, cursor=cursor)

            if pending and pending["status"] == "approved":
                db.decide_approval(pending["id"], "approved")

        db.update_task(task_id, status="succeeded", result="Task selesai tanpa pesan tambahan.", finished_at=db.now_iso())
        db.append_event(task_id, "completed", {"message": "Task selesai."})
        await self._cleanup(task_id)

    async def approve(self, approval_id: str, accepted: bool) -> dict[str, Any] | None:
        approval = db.get_approval(approval_id)
        if not approval:
            return None
        decision = "approved" if accepted else "rejected"
        updated = db.decide_approval(approval_id, decision)
        if updated:
            if accepted:
                await self.enqueue(approval["task_id"])
            else:
                db.update_task(approval["task_id"], status="cancelled", error="Approval rejected", finished_at=db.now_iso())
                await self._cleanup(approval["task_id"])
        return updated

    async def cancel(self, task_id: str, reason: str = "Cancelled by user") -> dict[str, Any] | None:
        task = db.get_task(task_id)
        if not task:
            return None
        db.update_task(task_id, status="cancelled", error=reason, finished_at=db.now_iso())
        db.append_event(task_id, "cancelled", {"reason": reason})
        await self._cleanup(task_id)
        return db.get_task(task_id)

    async def _cleanup(self, task_id: str) -> None:
        worker = self.sessions.pop(task_id, None)
        if worker:
            await worker.close()
