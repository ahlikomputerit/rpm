from __future__ import annotations

from typing import Any, Literal
from pydantic import BaseModel, Field, field_validator


TaskMode = Literal["read_only", "manual_approval"]


class TaskCreate(BaseModel):
    instruction: str = Field(min_length=3, max_length=4000)
    mode: TaskMode = "manual_approval"
    start_url: str | None = None
    allowed_domains: list[str] = Field(default_factory=list, max_length=10)

    @field_validator("allowed_domains")
    @classmethod
    def normalize_domains(cls, value: list[str]) -> list[str]:
        normalized: list[str] = []
        for domain in value:
            item = domain.strip().lower().replace("https://", "").replace("http://", "").strip("/")
            if item and item not in normalized:
                normalized.append(item)
        return normalized


class Action(BaseModel):
    tool: Literal[
        "browser.goto",
        "browser.click",
        "browser.type",
        "browser.press",
        "browser.scroll",
        "browser.wait",
        "browser.extract",
        "browser.screenshot",
        "task.complete",
    ]
    args: dict[str, Any] = Field(default_factory=dict)
    reason: str = Field(default="", max_length=500)
    expected_effect: str = Field(default="", max_length=500)
    requires_approval: bool = False


class AgentPlan(BaseModel):
    summary: str = Field(default="", max_length=1000)
    actions: list[Action] = Field(default_factory=list, max_length=40)


class ApprovalDecision(BaseModel):
    decision: Literal["approved", "rejected"]


class TaskControl(BaseModel):
    reason: str = Field(default="", max_length=300)
