from __future__ import annotations

from pathlib import Path


MIN_AVAILABLE_MB = 700


def available_memory_mb() -> int | None:
    meminfo = Path("/proc/meminfo")
    if not meminfo.exists():
        return None
    for line in meminfo.read_text(encoding="utf-8").splitlines():
        if line.startswith("MemAvailable:"):
            parts = line.split()
            return int(parts[1]) // 1024
    return None


def ensure_browser_capacity() -> None:
    available = available_memory_mb()
    if available is not None and available < MIN_AVAILABLE_MB:
        raise RuntimeError(f"Insufficient available memory for a browser worker: {available} MB available")
