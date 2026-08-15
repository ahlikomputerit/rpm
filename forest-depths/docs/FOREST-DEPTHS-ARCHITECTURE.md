# Forest Depths — Technical Architecture Summary

**Status:** Implemented feature summary  
**Project:** Forest Depths  
**Branch:** `forest-depths`  
**Latest feature commit:** `26b993f`  
**Document owner:** Manus AI  
**Last reviewed:** 2026-08-15

## 1. Executive summary

Forest Depths adalah pengalaman editorial scroll-driven satu halaman yang mengubah scroll menjadi perjalanan semakin dalam ke hutan tua. Implementasi aktual menggabungkan React 19, CSS atmospheric fallback, normalized scroll progress, lazy-loaded Three.js, PNG cutout nyata, Web Audio procedural ambience, chapter-local interactions, hidden idle observations, dan visual-effects layers.

Arsitektur utamanya mengikuti prinsip bahwa konten harus tetap lengkap tanpa WebGL, sedangkan Three.js menjadi enhancement yang menambah kedalaman ruang, parallax, kamera, fog, particles, dan object movement. Prinsip ini sesuai dengan requirement PRD bahwa image-based page harus tetap dapat dipahami ketika scene belum dimuat atau WebGL tidak tersedia [1].

> **Core rule:** Satu nilai `progress` ternormalisasi dari `0..1` menjadi sumber koordinasi untuk editorial chapter, depth rail, image crossfade, camera path, PNG object reveal, audio intensity, hidden detail, dan visual effects.

## 2. Technology stack

| Layer | Teknologi aktual | Peran |
|---|---|---|
| Application | React 19 + TypeScript | Page composition, state, lifecycle, semantic controls |
| Build | Vite 7 + esbuild | Development server, production bundle, server compatibility bundle |
| Package manager | pnpm | Dependency installation dan reproducible commands |
| Styling | Tailwind CSS 4 plus authored CSS | Global tokens, responsive layout, atmospheric effects, component states |
| Routing | Wouter | Lightweight client-side route shell |
| 3D | Three.js | Perspective camera, hybrid plate scene, procedural forest layers, PNG planes, particles, fog curtains |
| Audio | Web Audio API | Procedural ambience beds, crossfade, chapter cues, mute/unlock lifecycle |
| Icons | lucide-react | Navigation and editorial cue icons |
| Storage | Lifecycle-safe `/manus-storage/` URLs | Uploaded WebP, PNG, and brand mark assets |
| QA | TypeScript check, Vite build, browser preview | Static correctness, bundling, runtime and visual verification |

Forest tetap merupakan static frontend project. Tidak ada database, authentication, CMS, user account, atau server-side application state dalam fitur yang sudah diimplementasikan.

## 3. Source tree and component responsibilities

```text
forest-depths/
├── client/
│   ├── index.html
│   └── src/
│       ├── App.tsx
│       ├── index.css
│       ├── main.tsx
│       ├── pages/
│       │   └── Home.tsx
│       └── components/
│           ├── AudioDirector.tsx
│           ├── ThreeForestScene.tsx
│           ├── ErrorBoundary.tsx
│           └── ui/                 # Template primitives, mostly not central to Forest journey
├── docs/
│   ├── FOREST-DEPTHS.md
│   └── FOREST-DEPTHS-ARCHITECTURE.md
├── server/                         # Static-template compatibility placeholder
├── shared/                          # Static-template compatibility placeholder
├── package.json
└── forest-depths-execution-checklist.md
```

### 3.1 `App.tsx`

`App.tsx` menyediakan route shell, `ThemeProvider`, `TooltipProvider`, `Toaster`, dan `ErrorBoundary`. Route utama `/` menunjuk ke `Home`; route fallback menunjuk ke `NotFound`. Tidak ada business state pada level router karena seluruh journey state berada di `Home.tsx`.

### 3.2 `Home.tsx`

`Home.tsx` adalah orchestration layer. File ini memiliki empat tanggung jawab utama: mendefinisikan narrative data, menghitung scroll state, merender editorial shell, dan memasang optional subsystems.

Komponen lazy-loaded `ThreeForestScene` tidak di-import secara statis. `Home` menunggu browser idle atau timeout fallback sebelum mengaktifkan dynamic import. `Suspense fallback={null}` memastikan CSS/image experience tetap dapat tampil sebelum chunk Three.js siap.

`Home` juga memasang `AudioDirector` secara langsung dengan props `activeStage`, `progress`, dan `reducedMotion`. Dengan demikian audio tidak memiliki sumber kebenaran chapter terpisah.

### 3.3 `ThreeForestScene.tsx`

`ThreeForestScene` mengelola live environmental layer. Komponen ini melakukan WebGL probe, membuat renderer dengan `try/catch`, membatasi pixel ratio, memuat textured plates dan PNG textures, membangun camera paths, menggerakkan world groups, menjalankan render loop, dan membersihkan resource pada unmount.

Scene menggunakan `PerspectiveCamera`, `FogExp2`, hemisphere light, directional shaft light, textured planes, forest floor, trunks, roots, depth rings, PNG cutouts, dust, pollen, fireflies, leaf points, mist points, fog curtains, hidden glow, dan light orb.

### 3.4 `AudioDirector.tsx`

`AudioDirector` menyediakan suara sebagai enhancement opsional. Ia membuat tiga habitat beds procedural menggunakan filtered noise buffer: `edge`, `understory`, dan `heartwood`. Pergantian habitat menggunakan gain envelope sekitar 1,8 detik. Chapter transition cue menggunakan oscillator, filter, frequency profile, dan short envelope yang berbeda untuk sembilan stage.

AudioContext tidak dibuat saat first paint. Unlock dilakukan melalui pointer atau keyboard interaction. Tombol audio memiliki `aria-label`, `aria-pressed`, state `SOUND ON/OFF`, dan preference lokal `forest-depths-muted`. Reduced motion menonaktifkan cue tambahan dan memusatkan master gain ke silence.

### 3.5 `index.css`

`index.css` memuat design tokens, typography import, global environmental stack, tone variants, editorial layout, depth rail, interaction hotspot, audio control, hidden observation reveal, CSS particle fields, weather layers, leaf fall, fog of war, responsive rules, dan reduced-motion overrides.

CSS effects berada di belakang editorial content dan memakai `pointer-events: none`. Ini mencegah weather atau fog menangkap click, menutupi focus ring, atau mengganggu chapter navigation.

## 4. Narrative state model

### 4.1 Stable stage contract

`stages` adalah array sembilan record stabil. Setiap record memiliki `id`, `index`, `depth`, `meters`, `eyebrow`, `title`, `body`, `image`, `tone`, `note`, dan, jika relevan, `curiosity`, `transitionCue`, serta `cameraCharacter`.

| Index | Stable id | Depth | Tone | Camera character |
|---:|---|---:|---|---|
| 01 | `edge` | 12 m | `edge` | Slow lateral drift |
| 02 | `fern-passage` | 34 m | `fern` | Low push through leaves |
| 03 | `moss-creek` | 61 m | `creek` | Diagonal creek follow |
| 04 | `fog-basin` | 88 m | `fog` | Vertical lift through fog |
| 05 | `cathedral-grove` | 117 m | `grove` | Slow upward reveal |
| 06 | `thorn-hollow` | 143 m | `hollow` | Careful serpentine pass |
| 07 | `night-marsh` | 169 m | `marsh` | Floating marsh glide |
| 08 | `ancient-boundary` | 196 m | `boundary` | Orbit around stone |
| 09 | `heartwood` | 224 m | `heartwood` | Slow inward descent |

### 4.2 Normalized progress

Scroll state dihitung menggunakan:

```ts
const max = document.documentElement.scrollHeight - window.innerHeight;
const progress = max > 0 ? clamp(window.scrollY / max, 0, 1) : 0;
const active = Math.min(stages.length - 1, Math.floor(progress * stages.length));
```

`progress` disimpan di React state. Update scroll diproses melalui `requestAnimationFrame` saat motion normal dan diproses langsung saat reduced motion. Nilai ini kemudian digunakan untuk:

| Consumer | Mapping |
|---|---|
| Active stage | `floor(progress * stages.length)` |
| Plate crossfade | Jarak `scrubPosition` terhadap index plate |
| Depth rail | CSS variable `--depth-progress` |
| Depth readout | Interpolasi meter antar stage |
| Three.js camera | `chapterPosition = progress * (stageCount - 1)` |
| PNG plane reveal | Jarak progress terhadap target stage object |
| Audio master | Level `0.09 + progress * 0.07`, dibatasi maksimum `0.2` |
| Fog/weather | CSS opacity dan Three.js fog intensity |
| Hidden details | Creature selection berdasarkan active stage |

### 4.3 Chapter navigation

Chapter menu menggunakan semantic buttons. `jumpTo(index)` mencari element id stage dan memanggil `scrollIntoView`, dengan `behavior: auto` untuk reduced motion dan `smooth` untuk mode normal. Menu, brand button, hero CTA, story controls, dan return CTA semuanya memakai real buttons.

## 5. Editorial shell and fallback

Editorial shell berjalan di normal document flow dengan fixed environmental stack. Struktur utamanya adalah:

```text
forest-app
├── LazyThreeForestScene                optional enhancement
├── AudioDirector                       optional audio enhancement
├── environment                         fixed visual stack
│   ├── scene-stack                     crossfading WebP plates
│   ├── tone veil and vignette
│   ├── particle fields
│   ├── weather drift and leaf fall
│   ├── fog of war
│   └── grain
├── site-header                         brand, metadata, menu toggle
├── chapter-menu                        chapter buttons
├── depth-rail                          rail fill, dot, labels
├── main
│   ├── hero panel
│   ├── nine chapter story panels
│   └── closing panel
├── site-footer
├── creature-reveal                     idle observation
└── depth-readout                       active meters
```

Image fallback tidak menunggu Three.js. Setiap stage menggunakan WebP plate sebagai `background-image`; active plate dan neighboring plate dicrossfade berdasarkan scrub position. Ketika WebGL probe gagal, component scene berhenti sebelum renderer dibuat dan fixed image fallback tetap menjadi pengalaman utama.

## 6. Hybrid Three.js architecture

### 6.1 Initialization and guard

Scene melakukan langkah berikut secara berurutan:

1. Membuat `THREE.Scene`, fog, dan camera.
2. Memprobe `webgl2` atau `webgl` melalui canvas.
3. Membuat `WebGLRenderer` dalam `try/catch`.
4. Mengatur pixel ratio maksimum 1.7 desktop dan 1.2 pada viewport kecil.
5. Menambahkan canvas transparan dengan `aria-hidden="true"`.
6. Membuat world groups dan texture loaders.

Jika salah satu guard gagal, scene tidak melempar error ke root page.

### 6.2 Layer hierarchy

| Layer | Implementasi | Fungsi |
|---|---|---|
| Far | 9 WebP planes pada jarak z berbeda | Background plate hybrid dan parallax |
| Mid | Forest floor, depth rings, fog curtains | Ruang dan orientasi kedalaman |
| Near | PNG tree, rock, fern, mushroom planes | Object cutout nyata dengan reveal dan parallax |
| Structural | Trunk cylinders, roots, canopy group | Environmental scaffolding |
| Atmospheric | Dust, pollen, leaf points, mist points | Weather and depth texture |
| Luminous | Fireflies, hidden glow, light orb | Chapter cue dan idle detail |

PNG object hanya diberi opacity jika texture loader berhasil. Pada error load, object tidak ditampilkan sebagai placeholder visual.

### 6.3 Camera path

Setiap chapter memiliki camera path dengan `x`, `y`, `z`, `lookX`, `lookY`, `lookZ`, `roll`, `fov`, `swayX`, dan `swayY`. Path diinterpolasi memakai eased local progress. Transition pulse memengaruhi FOV, fog, plate z offset, frame rotation, dan world offset.

Camera position, lookAt target, roll, dan FOV berubah bersama progress sehingga camera tidak hanya menjadi dekorasi; ia mengubah perspektif ruang aktual.

### 6.4 Render loop

Render loop menghaluskan progress target menjadi `smoothProgress`. Pada motion normal digunakan interpolation `0.055`; pada reduced motion progress mengikuti target langsung. Setiap frame melakukan update terhadap plate opacity, PNG reveal, camera, fog density, light intensity, world movement, floor offset, forest frames, canopy drift, particles, fireflies, hidden glow, dan orb.

Cleanup membatalkan animation frame, menghapus resize listener, memanggil `renderer.dispose()`, men-dispose geometry/material melalui scene traversal, dan menghapus canvas.

## 7. Interaction and chapter narrative

`chapterInteractions` memiliki satu record untuk setiap non-hero story stage dan menggunakan asset PNG lifecycle-safe. Record berisi `asset`, `label`, `title`, `detail`, `x`, `y`, dan `kind`.

Hotspot dirender sebagai button dengan gambar PNG, pulse marker, label, `aria-expanded`, dan `aria-controls`. Click, tap, keyboard activation, hover, dan focus-visible semuanya memakai satu state `openInteraction`. Saat active chapter berubah, observation terbuka di-reset sehingga halaman tidak membawa konteks chapter sebelumnya.

Observation card menampilkan:

```text
FIELD OBSERVATION / chapter index
observation title
observation detail
```

Primary story copy tidak digantikan oleh observation. Ini menjaga narrative continuity ketika user tidak memakai interaction, WebGL, atau motion.

Idle hidden details berjalan terpisah dari hotspot. Setelah scroll berhenti sekitar 2,8 detik, `creature-reveal` menampilkan species-specific observation seperti fox, owl, deer, atau moth. Scroll baru mereset timer dan menyembunyikan detail.

## 8. Audio architecture

Audio bersifat opt-in dan tidak autoplay. `AudioDirector` membuat satu `AudioContext` setelah pointer atau keyboard event pertama. Tiga ambience habitat memakai satu shared noise buffer dengan filter berbeda:

| Habitat | Stage range | Filter profile |
|---|---:|---|
| `edge` | 01–03 | Low-pass sekitar 1200 Hz |
| `understory` | 04–06 | Band-pass sekitar 680 Hz |
| `heartwood` | 07–09 | Low-pass sekitar 330 Hz |

Saat habitat berganti, gain target setiap track dijadwalkan dengan time constant 1.8 detik. Master level mengikuti progress, tetapi mute dan reduced-motion dapat mengatur level menjadi zero. Setiap perubahan stage menghasilkan cue pendek dengan frequency dan low-pass filter yang berbeda.

Semua audio source dihentikan dan AudioContext ditutup saat unmount. Preference mute disimpan di localStorage dengan key `forest-depths-muted`.

## 9. Visual effects architecture

Visual effects dibagi menjadi dua jalur agar fallback tidak bergantung pada WebGL.

### CSS fallback

`weather-drift`, `weather-leaf-fall`, dan `fog-of-war` adalah fixed layers dengan `pointer-events: none`. Opacity dan background size berubah berdasarkan `--depth-progress` dan tone class. Fog dibuat lebih tebal pada chapter yang lebih dalam, tetapi direction gradient tetap menjaga teks dan controls terbaca.

### Three.js enhancement

Three.js menambahkan 150 leaf points, 90 mist points, dan tiga fog curtain planes. Leaf and mist transforms mengikuti elapsed time, tetapi amplitudo gerak menjadi zero pada reduced motion. Fog intensity memakai kombinasi depth dan transition pulse. Particle opacity diturunkan pada reduced motion dan kualitas mobile.

### Atmospheric budget

| Effect | Budget |
|---|---:|
| Dust points | 620 |
| Pollen points | 220 |
| Leaf points | 150 |
| Mist points | 90 |
| Fog curtains | 3 planes |
| Fireflies | 18 meshes |
| Forest depth rings | 7 rings |

Budget ini sengaja modest untuk mempertahankan visual richness tanpa menambah post-processing berat.

## 10. Asset pipeline and provenance

Asset besar tidak disimpan dalam source tree. File disimpan di `/home/ubuntu/webdev-static-assets/` selama proses kerja, dioptimalkan, lalu diunggah menggunakan lifecycle-safe storage. Source code hanya menyimpan URL `/manus-storage/...`.

| Asset class | Format | Usage |
|---|---|---|
| Scene plates | WebP | CSS fallback dan textured Three.js planes |
| Transparent objects | PNG alpha | Tree, fern, rock, mushroom, log, landmark, chapter hotspots |
| Brand mark | PNG transparent | Header, closing mark, favicon |
| Audio | Procedural Web Audio | Tidak ada large audio file tambahan |

Asset register mencatat source, license, dimensions, byte size, dan SHA-256 checksum. Candidate sources sebelumnya didokumentasikan melalui Kenney Nature Kit CC0, Poly Haven licensing, dan Unsplash license; provenance detail tetap berada di `forest-depths-asset-manifest.md` dan `forest-asset-research.md` [3] [4] [5].

Current visual plate coverage berjumlah enam distinct WebP candidates. Beberapa chapter masih menggunakan plate yang disengaja untuk reuse sampai dedicated thorn, marsh, dan heartwood plates tersedia. Ini adalah known limitation, bukan missing runtime asset.

## 11. Accessibility and resilience

| Concern | Current behavior |
|---|---|
| Semantic navigation | Header, nav, main, aside, footer, real buttons |
| Keyboard | Chapter buttons, menu, CTA, brand, audio, and hotspots reachable |
| Focus | Focus-visible hotspot and control states are preserved above effects |
| Audio | User-unlocked, mute button, ARIA state, local preference |
| Reduced motion | Three.js not loaded when preference is detected; CSS/Audio motion suppressed |
| No WebGL | Image plates, editorial copy, CSS effects, and interactions remain available |
| Asset failure | Texture-ready guard; failed object opacity stays zero; plate fallback color exists |
| Runtime failure | Three.js renderer creation is guarded; root page is protected by ErrorBoundary |
| Mobile | Responsive chapter layout, reduced particle opacity, renderer pixel ratio cap |
| Horizontal overflow | Story and hotspot widths use viewport-safe clamps and mobile caps |

## 12. Performance and QA

### Commands

```bash
pnpm install --frozen-lockfile
pnpm run check
pnpm run build
pnpm run dev
```

### Build result

`pnpm run check` dan `pnpm run build` berhasil setelah implementation visual effects terakhir. Production build menghasilkan separate `ThreeForestScene` chunk. Vite masih menampilkan warning bahwa beberapa chunks lebih besar dari 500 kB; warning ini terdokumentasi dan tidak merupakan build failure.

### Browser verification

QA yang sudah dilakukan mencakup initial desktop preview, chapter hotspot click, observation card open, controlled scroll ke progress 42%, visual effects mid-journey, depth rail, hidden observation, audio toggle rendering, dan browser console inspection. Initial dan mid-journey preview tidak menghasilkan fatal browser console error.

Browser scroll helper native dapat gagal bergerak karena fixed environmental shell. Untuk QA progress, gunakan chapter navigation button atau controlled `window.scrollTo`.

### Recommended follow-up QA

Mobile sekitar 390 px, `prefers-reduced-motion: reduce`, WebGL disabled, dan audio mute state perlu dijalankan sebagai dedicated regression pass setiap kali visual effects atau asset budget berubah. Dokumentasi visual verification disimpan di `forest-depths-visual-check.md`.

## 13. Known limitations

Pertama, Forest belum memiliki managed WebDev checkpoint/deployment terpisah; source implementation dan dokumentasi sudah dipisahkan pada branch GitHub `forest-depths`, tetapi project managed aktif sebelumnya adalah Ocean Depths.

Kedua, sebagian chapter masih memakai enam plate visual yang tersedia secara sengaja. Dedicated plates untuk thorn hollow, night marsh, dan heartwood masih merupakan asset follow-up.

Ketiga, ambience audio saat ini procedural, bukan field recording. Pendekatan ini menghindari transfer audio besar dan menjaga behavior lintas browser, tetapi karakter naturalistik dapat ditingkatkan dengan recorded assets tanpa mengubah interface `AudioDirector`.

Keempat, Three.js chunk masih besar menurut Vite warning. Dynamic import sudah memisahkan chunk dari initial route, tetapi granular code splitting lebih lanjut dapat dipertimbangkan setelah fitur P0 stabil.

Kelima, browser automation native scroll dan click kadang dapat mengalami stale session. Ini merupakan keterbatasan sesi automation yang dicatat dalam visual verification; initial render, controlled scroll, dan hotspot interaction telah diuji pada preview.

## 14. Status issue implementation

| Issue | Area | Status |
|---:|---|---|
| 01 | Design system and nine-stage state model | Implemented |
| 02 | Visual asset sourcing and optimization | Implemented with known plate coverage limitation |
| 03 | Editorial shell and CSS fallback | Implemented |
| 04 | Normalized forest journey progress | Implemented |
| 05 | Hybrid ForestScene | Implemented |
| 06 | Unique camera paths and cinematic scrubbing | Implemented |
| 07 | PNG objects and hidden forest details | Implemented |
| 08 | Forest audio director | Implemented |
| 09 | Lazy-loading, WebGL guard, cleanup | Implemented |
| 10 | Performance and asset budget audit | Partially implemented; ongoing mobile audit recommended |
| 11 | Desktop-mobile, accessibility, reduced-motion QA | Desktop and interaction pass implemented; dedicated mobile/reduced-motion regression remains recommended |
| 12 | Documentation and release checkpoint | Documentation implemented; managed Forest checkpoint pending project registration |

## 15. GitHub and documentation map

The implementation is maintained in the `forest-depths` branch of [`ahlikomputerit/rpm`](https://github.com/ahlikomputerit/rpm/tree/forest-depths). The latest visual effects commit is `26b993f`.

| File | Purpose |
|---|---|
| `forest-depths-PRD.md` | Product requirements and acceptance direction |
| `forest-depths-ISSUES.md` | Ordered issue plan and dependencies |
| `forest-depths-asset-manifest.md` | Asset roles, URLs, provenance, and optimization records |
| `forest-asset-research.md` | License and source research notes |
| `docs/FOREST-DEPTHS.md` | Technical runbook |
| `docs/FOREST-DEPTHS-ARCHITECTURE.md` | This architecture summary |
| `forest-depths-execution-checklist.md` | Implementation checklist and issue status |
| `forest-depths-visual-check.md` | Browser and visual verification notes |

## References

[1]: ../forest-depths-PRD.md "Forest Depths Product Requirements Document"
[2]: ../forest-depths-ISSUES.md "Forest Depths Issue Plan"
[3]: https://kenney.nl/assets/nature-kit "Kenney Nature Kit"
[4]: https://polyhaven.com/license "Poly Haven License"
[5]: https://unsplash.com/license "Unsplash License"
[6]: https://github.com/MengTo/kage "MengTo/Kage reference experience"
[7]: https://threejs.org/docs/ "Three.js documentation"
[8]: https://developer.mozilla.org/en-US/docs/Web/API/Window/requestIdleCallback "MDN requestIdleCallback"
