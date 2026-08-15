# Forest Depths Execution Checklist

## Completed foundation

- [x] PRD dan Issue Plan tersedia.
- [x] Asset sourcing dan lisensi diverifikasi.
- [x] Paket asset teroptimasi dan register dibuat.
- [x] Dokumentasi blueprint diunggah ke GitHub.

## Current build sequence

- [x] Salin scaffold `/home/ubuntu/ocean-depths` ke `/home/ubuntu/forest-depths` tanpa mengubah Ocean asli.
- [x] Ubah metadata, title, branding, dan dokumentasi lokal menjadi Forest Depths.
- [x] Buat branch atau direktori GitHub Forest terpisah dari artefak Ocean.
- [x] Selesaikan Issue #1: `ideas.md`, design tokens, typography, palette, motion, accessibility, dan nine-stage state model.
- [x] Selesaikan Issue #3: fixed environmental viewport, editorial shell, depth rail, chapter navigation, CTA, closing section, dan CSS fallback.
- [x] Upload optimized assets ke lifecycle-safe storage untuk project Forest.
- [x] Hubungkan chapter state dan normalized scroll progress.
- [x] Tambahkan lazy-loaded hybrid Three.js scene dengan WebGL-safe fallback.
- [x] Jalankan typecheck, production build, dan desktop screenshot/runtime pass; mobile/reduced-motion pass tetap menjadi validasi lanjutan.
- [x] Update dokumentasi GitHub pada branch `forest-depths` tanpa mencampur riwayat Ocean; managed checkpoint Forest menunggu project registration.

## Issue 8 — Forest audio director

- [x] Definisikan tiga ambience habitat dan sembilan chapter cue.
- [x] Implementasikan autoplay-safe AudioContext unlock dan optional ambience.
- [x] Tambahkan crossfade per habitat serta chapter transition cue.
- [x] Tambahkan mute button, keyboard state, local preference, dan ARIA labels.
- [x] Pastikan reduced-motion tidak memaksa cue tambahan atau motion audio.
- [x] Validasi first-load runtime, typecheck, build, cleanup path, dan dokumentasi GitHub lulus; direct click automation browser tercatat sebagai keterbatasan sesi stale, bukan fatal app error.

## Interaction and chapter narrative issue

- [x] Definisikan hotspot data per chapter dengan asset, label, detail, dan posisi depth.
- [x] Tambahkan interaction layer untuk hover, focus, tap, dan keyboard.
- [x] Tambahkan progressive narrative reveal yang terkoordinasi dengan active chapter.
- [x] Hubungkan hotspot ke PNG/plate nyata tanpa placeholder CSS object.
- [x] Pertahankan hidden idle details, reduced-motion, contrast, dan no-overflow behavior.
- [x] Jalankan typecheck/build, browser hotspot pass, dan dokumentasi; sinkronisasi GitHub menjadi langkah delivery berikutnya.
