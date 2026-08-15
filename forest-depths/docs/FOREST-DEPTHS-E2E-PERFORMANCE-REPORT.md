# Forest Depths — E2E and Mobile WebGL Performance Report

**Test date:** 2026-08-15  
**Target:** local Forest Depths preview at `http://localhost:3001`  
**Test environment:** Chromium browser session and headless Chromium CDP probes  
**Important limitation:** the sandbox reported SwiftShader/software rendering rather than a physical mobile GPU. Results below are valid for runtime resilience and software-rendering stress, but cannot certify stable FPS on Android/iOS hardware.

## 1. Executive result

The E2E journey passed for initial load, chapter menu, direct navigation to Heartwood, Heartwood hotspot observation, audio toggle, and reduced-motion fallback. TypeScript check and production build also passed. No application exception or fatal console error was observed.

The mobile performance requirement is **not certified** by this sandbox run. The browser's WebGL renderer on the interactive session was `ANGLE ... SwiftShader Device (Subzero)`, and the 5-second RAF sample recorded approximately 2.30 FPS with a 434.05 ms mean frame interval. The headless 390×844 probe recorded approximately 7.87 RAF FPS, but no Three.js canvas was mounted in that headless session because WebGL was unavailable. These measurements demonstrate that the sandbox is software-rendering or fallback-bound, not that a real mobile GPU will produce the same frame rate.

> **Conclusion:** Functional E2E behavior is passing. WebGL mobile frame-rate stability remains unverified and should be measured on a physical device or hardware-accelerated remote browser before claiming a target such as 30 or 60 FPS.

## 2. Test matrix

| Test | Method | Result | Evidence |
|---|---|---|---|
| Initial page load | Interactive browser navigation | Pass | Hero, nine chapters, depth rail, audio control, and effects rendered |
| Chapter menu | Open menu via visible button | Pass | Nine chapter buttons appeared and were reachable |
| Direct chapter navigation | Select chapter 09 | Pass | Heartwood view reached at approximately 88% progress |
| Hotspot observation | Click `HEARTWOOD GRAIN` | Pass | `FIELD OBSERVATION / 09` card opened |
| Audio toggle | Click `SOUND ON` at Heartwood | Pass | State changed to `SOUND OFF`, label changed to enable action |
| Console health | Browser console inspection | Pass | React DevTools info only; no fatal application exception |
| TypeScript | `pnpm run check` | Pass | `tsc --noEmit` completed successfully |
| Production build | `pnpm run build` | Pass with warning | Build completed; Three.js chunk warning remains documented |
| Reduced-motion fallback | Headless CDP 390×844 with media emulation | Pass | `canvasCount: 0`, hero present, 9 chapter buttons, audio control, scroll height 9347 px |
| Mobile viewport | Headless CDP device metrics 390×844 | Pass for layout probe | Metrics were applied; no application errors reported |
| Mobile WebGL FPS | Headless CDP RAF sample | Not certifiable | No canvas/renderer in headless probe; software-rendered interactive session was very slow |

## 3. Interactive E2E flow

The flow began from the initial page. The hero presented the Forest Depths title, the `ENTER THE FOREST` CTA, the depth rail, the chapter menu control, the audio control, and the first image plate. Opening the chapter menu exposed all nine chapter buttons without collapsing the page shell.

Selecting `09 THE HEARTWOOD` moved the document to the deepest narrative section. The active page state reported approximately 88% progress and a depth readout near 222 m. The Heartwood title, image plate, PNG hotspot, depth rail, and hidden creature observation remained synchronized.

Clicking `HEARTWOOD GRAIN` opened its observation card with the title **The deepest room is made of time.** The card appeared without removing the primary narrative text. Clicking the audio control changed `SOUND ON` to `SOUND OFF` and changed the accessible hint to `Enable forest sound`.

## 4. Frame pacing measurements

### 4.1 Interactive browser WebGL sample

A 5-second `requestAnimationFrame` sample was executed on the active browser preview while the interactive page had a Three.js canvas. The browser reported:

| Metric | Value |
|---|---:|
| Viewport | 1280 × 1100 CSS px |
| Device pixel ratio | 1 |
| Canvas | 1280 × 1100 px |
| Renderer | ANGLE, Vulkan, SwiftShader Device (Subzero) |
| Sample elapsed time | 5200.5 ms |
| RAF callbacks | 13 |
| Average measured FPS | 2.30 |
| Mean frame interval | 434.05 ms |
| P95 frame interval | 504.10 ms |
| P99 frame interval | 504.10 ms |
| Frames over 33.3 ms | 12 |
| Frames over 50 ms | 12 |
| Dropped-frame ratio using >20 ms threshold | 1.0000 |

The renderer string is the key interpretation signal. SwiftShader is software rendering in this environment; the result is not representative of a mobile GPU with hardware acceleration.

### 4.2 Headless mobile probe

The reproducible CDP probe applied a 390 × 844 device metric, device scale factor 1, touch emulation, and a 5-second RAF sample.

| Metric | Value |
|---|---:|
| Emulated viewport | 390 × 844 |
| Device pixel ratio | 1 |
| Canvas | Not mounted |
| Renderer | Not available |
| RAF callbacks | 41 |
| Average RAF rate | 7.87 FPS |
| Mean frame interval | 127.08 ms |
| P95 frame interval | 366.60 ms |
| P99 frame interval | 366.70 ms |
| Frames over 33.3 ms | 35 |
| Frames over 50 ms | 20 |
| Dropped-frame ratio using >20 ms threshold | 0.9000 |
| Captured page errors | None |

The absence of a canvas is consistent with the fallback contract under headless software constraints. It should not be interpreted as a successful WebGL performance test.

## 5. Reduced-motion and fallback result

The fallback probe emulated `prefers-reduced-motion: reduce` at 390 × 844. It reported `reducedMotion: true`, `canvasCount: 0`, hero text present, nine chapter navigation buttons, `SOUND ON`, and a 9347 px document height. This confirms that the page remains a complete scroll-driven editorial experience without loading the live Three.js layer.

The fallback path preserves the core content and controls while removing non-essential camera sway, particle movement, and chapter audio cue behavior. This is the expected behavior under the project accessibility contract.

## 6. Build and runtime health

The final source validation completed with:

```text
pnpm run check  → pass
pnpm run build  → pass
```

The production build emitted a separate `ThreeForestScene` chunk. Vite reported a chunk-size warning because the initial application and Three.js chunks are each above 500 kB after minification. This warning does not fail the build, but it remains a performance optimization candidate.

No fatal application console error was observed during the interactive browser pass. The only ordinary console output was the React DevTools informational message and the explicit frame probe result.

## 7. Interpretation and release recommendation

The release is functionally ready for further device QA: the primary journey, deep chapter navigation, hotspot observation, audio mute path, CSS fallback, and reduced-motion behavior passed. The frame-rate requirement is not yet release-certified for mobile because the available environment did not expose a hardware-accelerated mobile WebGL renderer.

Before claiming stable mobile FPS, run the same probe on at least one iOS Safari device and one Android Chrome device. Capture renderer, device model, DPR, canvas size, average FPS, P95 frame duration, and long-frame ratio while moving through chapters 01, 05, and 09. A practical acceptance target should be selected with the product owner; a 30 FPS floor is a more realistic first target for this layered experience than an unconditional 60 FPS guarantee.

If physical-device testing reveals low FPS, prioritize reductions in this order: lower mobile pixel ratio, reduce dust and pollen point counts, disable or defer fog curtain planes on low-memory devices, reduce texture plane count during transitions, and defer hidden creature visuals while scrolling. Do not remove the CSS fallback or narrative HTML as a performance shortcut.

## 8. Reproducible probes

The raw probe scripts and outputs are stored outside the project during execution:

```text
/home/ubuntu/forest-depths-mobile-probe.mjs
/home/ubuntu/forest-depths-mobile-probe.json
/home/ubuntu/forest-depths-fallback-probe.mjs
/home/ubuntu/forest-depths-fallback-probe.json
```

The visual notes are maintained in `forest-depths-visual-check.md`. The architecture and lifecycle assumptions are documented in `docs/FOREST-DEPTHS-ARCHITECTURE.md`.

## References

[1]: https://threejs.org/docs/ "Three.js documentation"
[2]: https://developer.mozilla.org/en-US/docs/Web/API/Window/requestAnimationFrame "MDN requestAnimationFrame"
[3]: https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia "MDN matchMedia"
