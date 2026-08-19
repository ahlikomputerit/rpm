from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


ROOT = Path(__file__).resolve().parents[1]
load_dotenv(ROOT / ".env")


@dataclass(frozen=True)
class Settings:
    app_name: str = os.getenv("AGENT_APP_NAME", "PCA-Lite")
    host: str = os.getenv("AGENT_HOST", "127.0.0.1")
    port: int = int(os.getenv("AGENT_PORT", "8787"))
    model_provider: str = os.getenv("AGENT_MODEL_PROVIDER", "demo")
    model_name: str = os.getenv("AGENT_MODEL", "gpt-4.1-mini")
    model_api_key: str = os.getenv("AGENT_MODEL_API_KEY", "")
    model_base_url: str = os.getenv("AGENT_MODEL_BASE_URL", "https://api.openai.com/v1")
    headless: bool = os.getenv("AGENT_HEADLESS", "true").lower() not in {"0", "false", "no"}
    max_steps: int = int(os.getenv("AGENT_MAX_STEPS", "20"))
    task_timeout_seconds: int = int(os.getenv("AGENT_TASK_TIMEOUT", "300"))
    max_tabs: int = int(os.getenv("AGENT_MAX_TABS", "3"))


settings = Settings()
