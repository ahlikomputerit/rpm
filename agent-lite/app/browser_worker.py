from __future__ import annotations

import asyncio
import json
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from .config import settings


class BrowserWorker:
    def __init__(self, task_id: str, workspace: Path, allowed_domains: list[str]):
        self.task_id = task_id
        self.workspace = workspace
        self.allowed_domains = allowed_domains
        self.playwright = None
        self.browser = None
        self.context = None
        self.page = None

    def _allowed(self, url: str) -> bool:
        if not self.allowed_domains:
            return False
        host = urlparse(url).hostname or ""
        return any(host == domain or host.endswith("." + domain) for domain in self.allowed_domains)

    async def start(self) -> None:
        from playwright.async_api import async_playwright

        self.workspace.mkdir(parents=True, exist_ok=True)
        self.playwright = await async_playwright().start()
        self.browser = await self.playwright.chromium.launch(headless=settings.headless)
        self.context = await self.browser.new_context(accept_downloads=False, viewport={"width": 1280, "height": 720})
        self.page = await self.context.new_page()
        self.page.set_default_timeout(12_000)

    async def close(self) -> None:
        if self.context:
            await self.context.close()
        if self.browser:
            await self.browser.close()
        if self.playwright:
            await self.playwright.stop()
        self.page = None
        self.context = None
        self.browser = None
        self.playwright = None

    async def execute(self, tool: str, args: dict[str, Any]) -> dict[str, Any]:
        if not self.page:
            raise RuntimeError("Browser worker is not started")

        if tool == "browser.goto":
            url = str(args.get("url", ""))
            if not url.startswith(("http://", "https://")):
                raise ValueError("Only http:// and https:// URLs are allowed")
            if not self._allowed(url):
                raise PermissionError(f"Domain is not allowlisted: {urlparse(url).hostname}")
            await self.page.goto(url, wait_until="domcontentloaded", timeout=20_000)
            return {"url": self.page.url, "title": await self.page.title()}

        if tool == "browser.click":
            selector = str(args.get("selector", ""))
            if not selector or len(selector) > 500:
                raise ValueError("A bounded CSS selector is required")
            await self.page.locator(selector).first.click()
            return {"url": self.page.url, "clicked": selector}

        if tool == "browser.type":
            selector = str(args.get("selector", ""))
            text = str(args.get("text", ""))
            if len(selector) > 500 or len(text) > 10_000:
                raise ValueError("Selector or text exceeds safety limit")
            await self.page.locator(selector).first.fill(text)
            return {"url": self.page.url, "typed_chars": len(text), "selector": selector}

        if tool == "browser.press":
            selector = str(args.get("selector", "body"))
            key = str(args.get("key", "Enter"))
            allowed_keys = {"Enter", "Escape", "Tab", "ArrowDown", "ArrowUp", "PageDown", "PageUp"}
            if key not in allowed_keys:
                raise ValueError(f"Key is not allowlisted: {key}")
            await self.page.locator(selector).first.press(key)
            return {"url": self.page.url, "pressed": key}

        if tool == "browser.scroll":
            amount = max(-1200, min(1200, int(args.get("amount", 700))))
            await self.page.mouse.wheel(0, amount)
            return {"url": self.page.url, "scrolled": amount}

        if tool == "browser.wait":
            seconds = max(0.1, min(5.0, float(args.get("seconds", 1))))
            await asyncio.sleep(seconds)
            return {"url": self.page.url, "waited": seconds}

        if tool == "browser.extract":
            selector = str(args.get("selector", "body"))
            max_chars = max(100, min(20_000, int(args.get("max_chars", 5000))))
            text = await self.page.locator(selector).first.inner_text()
            return {"url": self.page.url, "selector": selector, "text": text[:max_chars]}

        if tool == "browser.screenshot":
            path = self.workspace / f"step-{len(list(self.workspace.glob('step-*.png'))) + 1:03d}.png"
            await self.page.screenshot(path=str(path), full_page=False)
            return {"url": self.page.url, "artifact": str(path), "title": await self.page.title()}

        raise ValueError(f"Unsupported browser tool: {tool}")
