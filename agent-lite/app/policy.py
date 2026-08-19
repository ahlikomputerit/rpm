from __future__ import annotations

import hashlib
import json
from typing import Any

from .schemas import Action


HIGH_RISK_WORDS = {
    "submit",
    "send",
    "delete",
    "remove",
    "purchase",
    "buy",
    "publish",
    "consent",
    "accept",
    "login",
    "checkout",
}


def action_hash(action: Action) -> str:
    canonical = json.dumps(action.model_dump(), sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def requires_approval(action: Action) -> tuple[bool, str]:
    if action.requires_approval:
        return True, "Model menandai aksi ini membutuhkan persetujuan."
    haystack = json.dumps(action.model_dump(), ensure_ascii=False).lower()
    if any(word in haystack for word in HIGH_RISK_WORDS):
        return True, "Aksi mengandung pola yang dapat menimbulkan side effect."
    if action.tool == "browser.press" and str(action.args.get("key", "")) == "Enter":
        return True, "Tombol Enter dapat mengirim form atau mengubah state halaman."
    return False, "Aksi read-only atau observasi."


def approval_summary(action: Action) -> str:
    return f"{action.tool}: {action.reason or action.expected_effect or 'Aksi browser yang diusulkan'}"
