import { spawn } from "node:child_process";
import { setTimeout as delay } from "node:timers/promises";
import { writeFile } from "node:fs/promises";

const browser = spawn("chromium", ["--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-debugging-port=9224", "about:blank"], { stdio: "ignore" });
async function target() { for (let i = 0; i < 30; i += 1) { try { const r = await fetch("http://127.0.0.1:9224/json"); const t = await r.json(); if (t[0]?.webSocketDebuggerUrl) return t[0]; } catch {} await delay(100); } throw new Error("CDP target unavailable"); }
const socket = new WebSocket((await target()).webSocketDebuggerUrl);
await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
let id = 0; const pending = new Map();
socket.onmessage = (event) => { const m = JSON.parse(event.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } };
const cdp = (method, params = {}) => new Promise((resolve) => { const requestId = ++id; pending.set(requestId, resolve); socket.send(JSON.stringify({ id: requestId, method, params })); });
await cdp("Runtime.enable"); await cdp("Page.enable"); await cdp("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 1, mobile: true }); await cdp("Emulation.setEmulatedMedia", { features: [{ name: "prefers-reduced-motion", value: "reduce" }] });
await cdp("Page.navigate", { url: "http://127.0.0.1:3001/" }); await delay(3500);
const expression = `(() => ({ viewport: { width: innerWidth, height: innerHeight }, reducedMotion: matchMedia('(prefers-reduced-motion: reduce)').matches, canvasCount: document.querySelectorAll('canvas.forest-three-canvas').length, hero: document.querySelector('h1')?.textContent?.trim(), chapterButtons: document.querySelectorAll('.chapter-menu button').length, hotspotButtons: document.querySelectorAll('.chapter-interaction').length, audioButton: document.querySelector('.audio-toggle')?.textContent?.trim(), bodyHeight: document.documentElement.scrollHeight }))()`;
const response = await cdp("Runtime.evaluate", { expression, returnByValue: true });
const report = { mode: "headless reduced-motion mobile fallback", result: response.result?.result?.value ?? null };
await writeFile("/home/ubuntu/forest-depths-fallback-probe.json", JSON.stringify(report, null, 2)); console.log(JSON.stringify(report, null, 2)); socket.close(); browser.kill();
