import { spawn } from "node:child_process";
import { setTimeout as delay } from "node:timers/promises";
import { writeFile } from "node:fs/promises";

const browser = spawn("chromium", [
  "--headless=new",
  "--no-sandbox",
  "--disable-dev-shm-usage",
  "--use-gl=swiftshader",
  "--remote-debugging-port=9223",
  "--window-size=390,844",
  "about:blank",
], { stdio: "ignore" });

async function getTarget() {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    try {
      const response = await fetch("http://127.0.0.1:9223/json");
      const targets = await response.json();
      if (targets[0]?.webSocketDebuggerUrl) return targets[0];
    } catch {}
    await delay(100);
  }
  throw new Error("CDP target unavailable");
}

const target = await getTarget();
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
let id = 0;
const pending = new Map();
socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.id && pending.has(message.id)) { pending.get(message.id)(message); pending.delete(message.id); }
};
const cdp = (method, params = {}) => new Promise((resolve) => {
  const requestId = ++id;
  pending.set(requestId, resolve);
  socket.send(JSON.stringify({ id: requestId, method, params }));
});

await cdp("Runtime.enable");
await cdp("Page.enable");
await cdp("Network.enable");
await cdp("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
await cdp("Emulation.setTouchEmulationEnabled", { enabled: true });
const errors = [];
socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.method === "Runtime.exceptionThrown") errors.push({ type: "exception", detail: message.params.exceptionDetails?.text });
  if (message.method === "Log.entryAdded" && ["error", "warning"].includes(message.params.entry.level)) errors.push({ type: message.params.entry.level, text: message.params.entry.text });
  if (message.id && pending.has(message.id)) { pending.get(message.id)(message); pending.delete(message.id); }
};
await cdp("Page.navigate", { url: "http://127.0.0.1:3001/" });
await delay(5000);
const expression = `(() => new Promise((resolve) => {
  const canvas = document.querySelector('canvas.forest-three-canvas');
  const gl = canvas?.getContext('webgl2') || canvas?.getContext('webgl');
  const debug = gl?.getExtension('WEBGL_debug_renderer_info');
  const start = performance.now(); const samples = []; let last = start; let frames = 0;
  const tick = (now) => { const delta = now - last; if (frames > 0) samples.push(delta); last = now; frames += 1;
    if (now - start < 5000) requestAnimationFrame(tick); else {
      const sorted = samples.slice().sort((a,b) => a-b); const mean = samples.reduce((a,b) => a+b, 0) / Math.max(1, samples.length);
      const pct = (p) => sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * p))] || 0;
      resolve({ viewport: { width: innerWidth, height: innerHeight, dpr: devicePixelRatio }, canvas: canvas ? { width: canvas.width, height: canvas.height, cssWidth: canvas.clientWidth, cssHeight: canvas.clientHeight } : null, renderer: gl && debug ? gl.getParameter(debug.UNMASKED_RENDERER_WEBGL) : gl ? gl.getParameter(gl.RENDERER) : null, frames, elapsedMs: now - start, avgFps: Number((1000 / mean).toFixed(2)), meanFrameMs: Number(mean.toFixed(2)), p95FrameMs: Number(pct(0.95).toFixed(2)), p99FrameMs: Number(pct(0.99).toFixed(2)), longFramesOver33ms: samples.filter((x) => x > 33.3).length, longFramesOver50ms: samples.filter((x) => x > 50).length, droppedFrameRatio: Number((samples.filter((x) => x > 20).length / Math.max(1, samples.length)).toFixed(4)) });
    }
  }; requestAnimationFrame(tick);
}))()`;
const result = await cdp("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true });
const report = { mode: "headless mobile emulation", target: "390x844", result: result.result?.result?.value ?? null, errors };
await writeFile("/home/ubuntu/forest-depths-mobile-probe.json", JSON.stringify(report, null, 2));
socket.close(); browser.kill();
console.log(JSON.stringify(report, null, 2));
