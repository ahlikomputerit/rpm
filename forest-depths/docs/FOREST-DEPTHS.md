# Forest Depths — Technical Runbook

Forest Depths is a scroll-driven biophilic editorial experience that moves from the forest edge into ancient heartwood. The implementation keeps the narrative readable without WebGL and adds a lazy-loaded Three.js enhancement with textured scene plates, transparent PNG depth objects, woodland particles, fog, roots, and firefly cues.

## Current architecture

The page lives in `client/src/pages/Home.tsx`. A single normalized `progress` value drives the active stage, local stage interpolation, depth rail, plate crossfade, object opacity, and `ThreeForestScene`. The nine stable stages are `edge`, `fern-passage`, `moss-creek`, `fog-basin`, `cathedral-grove`, `thorn-hollow`, `night-marsh`, `ancient-boundary`, and `heartwood`.

The editorial shell is intentionally complete without Three.js. It contains the fixed environment stack, header, chapter menu, depth rail, hero, nine narrative chapters, closing section, keyboard-reachable buttons, responsive rules, reduced-motion behavior, and atmospheric CSS veil.

## Asset pipeline

Large media is stored outside the source tree and uploaded through lifecycle-safe storage. Scene plates are WebP candidates sourced from Unsplash. Transparent depth objects are optimized PNG files sourced from Kenney Nature Kit CC0. Asset metadata, source URLs, licenses, dimensions, and SHA-256 checksums are recorded in the root `forest-depths-asset-manifest.md` and `asset-register.json`.

## Hybrid scene

`client/src/components/ThreeForestScene.tsx` is dynamically imported after first paint. It guards WebGL initialization, caps pixel ratio, loads plate textures and PNG object textures with failure-safe opacity, and disposes renderer resources on unmount. The live environment uses trunk/root meshes only for environmental scaffolding; detailed visible objects remain real uploaded PNG assets.

The camera paths remain chapter-specific and are interpolated with eased local progress. Forest-specific motion includes canopy drift, root-floor parallax, fog-density changes, woodland dust, firefly pulses, and a slow inward heartwood descent.

## Interaction and chapter narrative

Each non-hero chapter owns one `chapterInteractions` record in `client/src/pages/Home.tsx`. The record maps a real uploaded PNG asset to a label, observation title, detail copy, position, and semantic kind. The interaction layer renders the PNG as a keyboard/touch-safe button with `aria-expanded` and `aria-controls`, then reveals a chapter-local observation card without replacing or hiding the primary narrative.

The active chapter resets any previously open observation so the page does not carry stale context across a scroll transition. The active story heading and body receive a restrained narrative-rise reveal, while the hotspot image uses a subtle scale and pulse. Reduced motion disables the pulse and reveal animation but preserves all text, controls, and focus behavior. The observation copy remains in the DOM for accessible reading and WebGL-free fallback.

## Audio director

`client/src/components/AudioDirector.tsx` creates three optional woodland ambience beds with Web Audio filtered noise: edge air and canopy, understory creek/leaf texture, and heartwood low-frequency room tone. The active bed is selected from the same chapter index used by the scroll journey and crossfaded over approximately 1.8 seconds. Each chapter transition emits a short, quiet oscillator cue with a chapter-specific frequency and filter profile.

Audio is never started during page load. The first pointer or keyboard interaction creates the AudioContext, starts the current habitat bed, and leaves the user in control through the fixed `SOUND ON` / `SOUND OFF` button. The preference is stored under `forest-depths-muted`. Reduced motion suppresses chapter cues and sets the master audio level to zero rather than forcing extra sensory motion. All sources and the AudioContext are stopped and closed during cleanup.

## Validation

Run `pnpm install --frozen-lockfile`, `pnpm run check`, and `pnpm run build`. The build should emit a separate `ThreeForestScene` chunk. Verify the page at desktop and approximately 390px mobile width, with reduced motion enabled and WebGL unavailable. A bundle-size warning may remain because Three.js is intentionally kept in a separate lazy chunk.

## Known follow-ups

The current sourced plate set covers six visual candidates and is reused intentionally for the remaining chapter tones until dedicated thorn, marsh, and heartwood plates are sourced. The audio beds are intentionally procedural and do not require additional large media files; dedicated field recordings can replace them later without changing the chapter contract.
