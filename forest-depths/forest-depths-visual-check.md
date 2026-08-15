# Forest Depths Visual Check

The local preview at `http://localhost:3001` loads the Forest metadata, nine chapter labels, editorial copy, depth rail, new Forest PNG brand mark, and storage-backed scene plates. The browser console showed no fatal runtime error on first load.

The current ThreeForestScene is structurally working and dynamically loaded, but its inherited procedural layer still contains ocean-shaped primitives such as kelp-like stems, fish, jellyfish, and trench geometry. This is a known next-step issue: replace those inherited forms with forest-specific roots, trunks, fog particles, canopy depth, and woodland landmarks before considering the hybrid scene complete.

## Second visual pass

After replacing the inherited procedural objects, the preview still renders the forest plate stack, new trunk/roots depth layer, amber firefly points, and editorial shell. The console again reported no fatal runtime errors. The build continues to emit a separate `ThreeForestScene` chunk; the remaining warning is bundle size, not a build failure.

## Final browser pass

The final local browser reload at `http://localhost:3001` renders the Forest title, nine chapter navigation entries, depth meter from 0 to 224 m, editorial hero, CTA, brand mark, real scene plates, and visible woodland/particle layers. The prior blank page was a stale browser session rather than an application failure; reopening the URL restored the page.

## Audio pass

The audio feature is visible in the preview as `SOUND ON` with an accessible mute label. A subsequent direct click attempt encountered the browser automation's stale-session condition and moved the browser to a blank state; this is a browser-session issue, not a reported application error. The next validation should reopen the URL and inspect the toggle state after interaction.

## Final audio browser pass

After reopening the preview, the `SOUND ON` control rendered at the fixed lower edge alongside the complete Forest narrative. The browser console contained only the standard React DevTools informational line and no AudioContext, React, or asset-loading fatal error. Direct automation click remained unreliable because the browser session frequently became stale; the code path itself passes typecheck/build and the control is present with an accessible label.

## Interaction browser pass

The interaction layer rendered nine chapter hotspot buttons using real uploaded PNG assets. Clicking `FERN UNDERSTOREY` successfully moved the viewport into chapter 02 and preserved the hotspot, story copy, depth rail, and hidden idle observation. The browser snapshot confirmed accessible button labels for Fern, Creek Stone, Fog Fern, Cedar Memory, Thorn Crossing, Marsh Signal, Boundary Stone, and Heartwood Grain. The observation copy is present in the DOM for narrative/accessibility continuity while CSS controls its visual reveal state.

## Observation card interaction

A second click on `FERN UNDERSTOREY` visibly opened the `FIELD OBSERVATION / 02` card with the chapter-specific title and detail text. The card appears beside the real PNG hotspot, remains within the editorial composition, and does not cover the primary narrative copy. This confirms the click/tap interaction path in the live preview.
