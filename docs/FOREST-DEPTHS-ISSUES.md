# Forest Depths — Issue Plan

Issue dibuat berurutan. Nomor di bawah adalah urutan kerja dan bukan nomor GitHub final. Issue berikutnya baru boleh dimulai setelah acceptance criteria issue sebelumnya terpenuhi.

## Issue 1 — [P0] Finalize design system and narrative state model

**Dependency:** Tidak ada.

**Tujuan:** Mengubah PRD menjadi `ideas.md`, tone map, stage data contract, typography system, copy voice, dan naming final untuk sembilan chapter.

**Deliverable:** `ideas.md`, stage type, nine-stage content table, tone palette, camera-path brief, dan decision log.

**Acceptance criteria:** Semua sembilan chapter memiliki id stabil, depth label, title, body, image slot, tone, note, transition cue, dan camera character. Tiga alternatif desain terdokumentasi, satu arah dipilih, dan keputusan final konsisten dengan Biophilic Editorial.

## Issue 2 — [P0] Prepare and optimize forest visual assets

**Dependency:** Issue 1.

**Tujuan:** Membuat seluruh asset visual sebelum coding scene.

**Deliverable:** Hero/reference image, zone plates, logo mark, transparent PNG cutouts, fog/leaf overlays, landmark assets, dan asset manifest.

**Acceptance criteria:** Setiap major zone memiliki asset berbeda atau fallback yang disengaja. Asset transparan memiliki alpha bersih. Asset dikompres, ukuran dicatat, URL storage valid, dan tidak ada generation placeholder yang dapat terlihat publik.

## Issue 3 — [P0] Build accessible editorial shell and CSS fallback

**Dependency:** Issue 1 dan Issue 2.

**Tujuan:** Membuat pengalaman lengkap tanpa Three.js.

**Deliverable:** Header, hero, chapter sections, depth rail, CTA, closing section, keyboard navigation, responsive layout, atmospheric CSS, dan fallback image stack.

**Acceptance criteria:** Halaman tetap memiliki narasi lengkap tanpa WebGL. Text contrast aman terhadap background aktual. Navigasi dapat digunakan keyboard. Layout tidak menghasilkan horizontal overflow pada viewport sekitar 390px.

## Issue 4 — [P0] Implement normalized forest journey progress

**Dependency:** Issue 3.

**Tujuan:** Menjadikan satu normalized progress sebagai sumber koordinasi seluruh sistem.

**Deliverable:** Scroll progress hook/state, active chapter mapping, local chapter progress, depth rail fill, and chapter navigation sync.

**Acceptance criteria:** Scroll position, active chapter, rail, editorial content, dan anchor navigation tidak saling tertinggal. Progress stabil saat resize dan tidak membuat render loop tak terbatas.

## Issue 5 — [P0] Implement hybrid ForestScene Three.js

**Dependency:** Issue 2 dan Issue 4.

**Tujuan:** Membuat dunia 3D live yang menjadi enhancement utama.

**Deliverable:** `ForestScene.tsx`, PerspectiveCamera, hybrid image plates, far/mid/near layers, fog, lights, forest floor, roots, procedural terrain, and initial particles.

**Acceptance criteria:** Background plate berada dalam scene 3D, bukan hanya CSS fixed layer. Kamera bergerak melalui ruang saat scroll. Foreground dan background memiliki parallax berbeda. WebGL failure tidak membuat page crash.

## Issue 6 — [P0] Add unique camera paths and cinematic scrubbing

**Dependency:** Issue 5.

**Tujuan:** Memberi karakter gerak berbeda pada setiap chapter.

**Deliverable:** Nine camera paths, look targets, FOV, roll, easing, transition pulse, and chapter-local scrubbing.

**Acceptance criteria:** Tidak ada dua chapter yang memiliki motion profile identik. Perpindahan antar chapter kontinu, tidak patah, dan tetap terbaca. Reduced-motion menonaktifkan motion tambahan.

## Issue 7 — [P1] Integrate PNG depth objects and hidden forest details

**Dependency:** Issue 5 dan Issue 2.

**Tujuan:** Menambahkan detail object berkualitas tinggi dan curiosity beats.

**Deliverable:** Fern, branch, moss, rocks, fireflies, owl/deer silhouette, landmark stone, and texture-ready object loader.

**Acceptance criteria:** Object hanya terlihat setelah texture siap. Object memiliki stage/depth target, parallax, opacity interpolation, dan fallback aman. Hidden detail muncul setelah idle-scroll tanpa menutupi copy utama.

## Issue 8 — [P1] Add forest audio director

**Dependency:** Issue 4 dan Issue 6.

**Tujuan:** Menyamakan ambience dan cue dengan zona serta camera character.

**Deliverable:** AudioDirector, ambience groups, chapter cue map, crossfade, mute control, local preference, and autoplay unlock.

**Acceptance criteria:** Audio tidak autoplay tanpa interaction. Tombol mute dapat diakses keyboard. Ambience berubah secara halus. Reduced-motion tidak memaksa cue tambahan.

## Issue 9 — [P0] Add lazy-loading, WebGL guard, and lifecycle cleanup

**Dependency:** Issue 5, Issue 7, dan Issue 8.

**Tujuan:** Menjaga first paint, reliability, dan cleanup.

**Deliverable:** Dynamic import, idle scheduler fallback, WebGL probe, renderer try/catch, pixel ratio cap, asset failure handling, and dispose lifecycle.

**Acceptance criteria:** Initial app chunk tidak memuat Three.js penuh. Halaman tampil sebelum scene siap. WebGL disabled tetap menampilkan fallback. Tidak ada fatal console error setelah mount/unmount atau resize.

## Issue 10 — [P0] Performance and asset budget audit

**Dependency:** Issue 9.

**Tujuan:** Mengukur dan menurunkan biaya rendering serta transfer.

**Deliverable:** Asset budget table, compressed asset set, geometry budget, pixel ratio policy, network audit, and performance notes.

**Acceptance criteria:** Semua asset major berada dalam budget yang disepakati. Texture tidak memuat placeholder. Build menunjukkan Three.js chunk terpisah. Mobile tidak memakai quality setting desktop penuh.

## Issue 11 — [P0] Desktop-mobile, accessibility, and reduced-motion QA

**Dependency:** Issue 10.

**Tujuan:** Memvalidasi pengalaman pada kondisi utama pengguna.

**Deliverable:** QA checklist, desktop screenshot, mobile screenshot, keyboard pass, reduced-motion pass, audio-muted pass, and WebGL fallback pass.

**Acceptance criteria:** Tidak ada horizontal overflow, text collision, broken chapter navigation, fatal runtime error, atau loss of narrative. Temuan QA dicatat sebagai follow-up issue atau diperbaiki sebelum close.

## Issue 12 — [P0] Documentation and release checkpoint

**Dependency:** Issue 11.

**Tujuan:** Menutup siklus produk dengan dokumentasi dan checkpoint yang dapat direproduksi.

**Deliverable:** Technical documentation, asset manifest, runbook, known limitations, changelog, and release checkpoint.

**Acceptance criteria:** Dokumentasi menjelaskan arsitektur aktual, perintah build, fallback, asset pipeline, troubleshooting, dan known warnings. Checkpoint hanya dibuat setelah build serta QA pass.

## Urutan dependency ringkas

```text
1 Design/state
  └── 2 Assets
        └── 3 Editorial fallback
              └── 4 Progress model
                    └── 5 Hybrid ForestScene
                          ├── 6 Camera scrubbing
                          ├── 7 PNG objects
                          └── 8 Audio
                                └── 9 Lazy-load/guards/cleanup
                                      └── 10 Performance budget
                                            └── 11 QA
                                                  └── 12 Docs/release
```

## Label yang digunakan

Gunakan label `forest-depths`, `prd`, `p0`, `p1`, `design`, `assets`, `frontend`, `threejs`, `audio`, `performance`, `accessibility`, `qa`, dan `documentation`. Jika label belum tersedia, buat label tersebut sebelum issue dibuat atau gunakan label GitHub yang sudah tersedia dengan makna terdekat.
