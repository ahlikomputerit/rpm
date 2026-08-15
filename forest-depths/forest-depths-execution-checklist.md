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

## Visual effects issue

- [x] Definisikan budget efek, intensitas per chapter, dan prioritas fallback.
- [x] Tambahkan CSS weather veil, leaf/rain/dust field, dan fog-of-war fallback.
- [x] Tambahkan progress-aware particle and fog layers ke ThreeForestScene.
- [x] Pastikan effects tidak menutupi copy, hotspot, controls, atau focus ring.
- [x] Terapkan reduced-motion dan mobile quality cap.
- [x] Jalankan typecheck/build dan browser pass; sinkronisasi GitHub menjadi langkah delivery berikutnya.

## Technical architecture documentation

- [x] Inventaris komponen React, Three.js, CSS, audio, asset, dan state model.
- [x] Dokumentasikan alur normalized progress dari scroll ke semua subsystem.
- [x] Dokumentasikan lifecycle, fallback, accessibility, reduced-motion, dan performance budget.
- [x] Tulis ringkasan arsitektur teknis lengkap beserta known limitations dan command QA.
- [x] Validasi dokumentasi terhadap source aktual; sinkronisasi branch GitHub Forest menjadi langkah delivery berikutnya.

## E2E and mobile WebGL performance QA

- [x] Jalankan E2E initial load, chapter navigation, Heartwood hotspot observation, audio toggle, dan closing content verification.
- [x] Ukur frame count, average FPS, p95 frame duration, long frames, dan RAF continuity; mobile hardware FPS masih tidak tersertifikasi karena sandbox memakai SwiftShader/fallback.
- [x] Catat renderer, pixel ratio, canvas size, canvas availability, console errors, dan build/runtime warnings.
- [x] Jalankan reduced-motion mobile fallback, WebGL-unavailable behavior, reload/initial mount, dan resize policy review; physical resize/GPU pass tetap direkomendasikan.
- [x] Tulis QA report dengan batasan eksplisit bahwa sandbox mobile viewport bukan perangkat fisik.
- [x] Sinkronkan report dan raw measurements ke branch GitHub Forest.

## Physical device WebGL testing guide

- [x] Tetapkan matrix perangkat Android/iOS dan kondisi baseline.
- [x] Dokumentasikan setup hosting, device preparation, hardware acceleration, dan network conditions.
- [x] Tulis prosedur pengukuran FPS/frame time/chapter stress/thermal behavior.
- [x] Tetapkan acceptance criteria dan severity classification.
- [x] Tambahkan troubleshooting untuk WebGL fallback, remote debugging, Safari, thermal throttling, dan asset failures.
- [x] Sertakan template test report serta referensi resmi.
- [x] Sinkronkan panduan ke branch GitHub Forest.

## Public preview deployment

- [ ] Jalankan server Forest pada port publik sementara.
- [ ] Validasi URL publik, initial render, assets, dan runtime console.
- [ ] Laporkan perbedaan preview sementara dan managed deployment permanen.

## CI/CD deployment configuration

- [x] Tambahkan GitHub Actions quality-gate workflow.
- [x] Tambahkan workflow deploy Vercel dengan preview dan production paths.
- [x] Tambahkan workflow deploy Netlify sebagai provider alternatif.
- [x] Tambahkan `vercel.json`, `netlify.toml`, dan dokumentasi secrets/rollback.
- [x] Jalankan check/build serta audit konfigurasi workflow secara lokal.
- [ ] Sinkronkan konfigurasi CI/CD ke branch GitHub Forest.
