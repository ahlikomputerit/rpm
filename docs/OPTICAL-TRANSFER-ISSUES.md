# Lumen Transfer — Issue Blueprint

**Blueprint sumber:** [`docs/OPTICAL-TRANSFER-PRD.md`](./OPTICAL-TRANSFER-PRD.md)  
**Status:** Siap dibuat sebagai GitHub issue  
**Urutan:** Dependency-driven, P0 lebih dahulu  

## Prinsip pemecahan issue

Setiap issue memiliki deliverable yang dapat direview dan acceptance criteria yang dapat diuji. Issue tidak dianggap selesai hanya karena aplikasi berhasil dikompilasi. Untuk issue yang menyentuh kamera atau performa, pengujian pada perangkat Android nyata wajib dilakukan karena browser-based emulator Google AI Studio tidak menyediakan camera/photo capture.[1]

| Urutan | Judul | Prioritas | Dependency |
|---|---|---:|---|
| 01 | [#13](https://github.com/ahlikomputerit/rpm/issues/13) — Establish Android project foundation and navigation | P0 | Tidak ada |
| 02 | [#14](https://github.com/ahlikomputerit/rpm/issues/14) — Define protocol envelope, metadata, and checksum contract | P0 | 01 |
| 03 | [#15](https://github.com/ahlikomputerit/rpm/issues/15) — Spike QR codec and JVM-compatible decoder | P0 | 01 |
| 04 | [#16](https://github.com/ahlikomputerit/rpm/issues/16) — Implement sender file selection and animated QR flow | P0 | 02, 03 |
| 05 | [#17](https://github.com/ahlikomputerit/rpm/issues/17) — Implement receiver camera permission and live QR scanning | P0 | 02, 03 |
| 06 | [#18](https://github.com/ahlikomputerit/rpm/issues/18) — Reconstruct, verify, and save received files | P0 | 02, 05 |
| 07 | [#19](https://github.com/ahlikomputerit/rpm/issues/19) — Add loss-tolerant fountain-code transfer | P0 | 04, 05, 06 |
| 08 | [#20](https://github.com/ahlikomputerit/rpm/issues/20) — Add transfer state machine, cancellation, and recovery | P0 | 04, 05, 06 |
| 09 | [#21](https://github.com/ahlikomputerit/rpm/issues/21) — Add diagnostics and repeatable device benchmark | P1 | 04, 05, 07, 08 |
| 10 | [#22](https://github.com/ahlikomputerit/rpm/issues/22) — Add privacy copy, accessibility, screen-on, and UX hardening | P0/P1 | 04, 05, 08 |
| 11 | [#23](https://github.com/ahlikomputerit/rpm/issues/23) — Perform two-device offline QA and release hardening | P0 | 07, 08, 09, 10 |
| 12 | [#24](https://github.com/ahlikomputerit/rpm/issues/24) — Document installation, AI Studio handoff, and known limitations | P0 | 11 |

## Issue detail

### Issue 01 — [P0] Establish Android project foundation and navigation

**Dependency:** Tidak ada.  
**Tujuan:** Membuat skeleton Kotlin + Jetpack Compose yang sesuai batasan Google AI Studio: single activity, single module, dan client-side only.  
**Deliverable:** Project Gradle, Compose theme, `HomeScreen`, navigation antara Send dan Receive, placeholder state, minimum README, dan build instructions.  
**Acceptance criteria:** Project dapat dibuka di Google AI Studio dan Android Studio; debug build berhasil; Home memiliki route Kirim dan Terima; konfigurasi tidak menambahkan backend, Firebase, atau NDK; theme mendukung portrait, landscape, dark mode, dan accessible contrast.  

### Issue 02 — [P0] Define protocol envelope, metadata, and checksum contract

**Dependency:** Issue 01.  
**Tujuan:** Membakukan wire format agar sender dan receiver dapat dikembangkan terpisah.  
**Deliverable:** Data classes, serializer, parser, protocol version, magic prefix, frame kinds, transfer ID, file metadata, SHA-256 contract, error codes, dan golden vectors.  
**Acceptance criteria:** Envelope valid dapat di-serialize lalu di-parse kembali tanpa kehilangan data; input random atau truncated ditolak dengan error terstruktur; checksum file dan checksum frame memiliki definisi berbeda; golden vectors tersimpan sebagai test fixture; perubahan version tidak diam-diam diterima.  

### Issue 03 — [P0] Spike QR codec and JVM-compatible decoder

**Dependency:** Issue 01.  
**Tujuan:** Memastikan library QR encoder dan decoder yang digunakan kompatibel dengan Kotlin/Compose, Maven dependency, dan batasan AI Studio yang tidak mendukung NDK/native C++.  
**Deliverable:** Decision record, encoder wrapper, decoder wrapper atau prototype, sample frame screen, camera feasibility note, dan dependency license note.  
**Acceptance criteria:** Encoder menghasilkan QR dari payload protocol; decoder dapat membaca sample payload; library tidak membutuhkan NDK untuk jalur MVP; dependency dapat di-resolve melalui Gradle; keterbatasan emulator dicatat dan satu uji kamera dijalankan pada perangkat fisik.  

### Issue 04 — [P0] Implement sender file selection and animated QR flow

**Dependency:** Issue 02 dan Issue 03.  
**Tujuan:** Memungkinkan pengguna memilih file dan menampilkannya sebagai frame QR berulang.  
**Deliverable:** Android system file picker integration, metadata reader, SHA-256 worker, sequential chunk adapter, QR renderer, frame timer, pause/resume, cancel, screen-on lifecycle, dan sender UI.  
**Acceptance criteria:** File gambar, PDF, audio, video kecil, teks, dan biner dapat dipilih; nama, MIME type, ukuran, dan checksum ditampilkan; QR mengulang frame tanpa UI freeze; pause/resume dan cancel bekerja; layar tidak sleep selama sending; sender tidak membuat network request.  

### Issue 05 — [P0] Implement receiver camera permission and live QR scanning

**Dependency:** Issue 02 dan Issue 03.  
**Tujuan:** Membaca frame QR dari kamera perangkat secara real-time dan mengubahnya menjadi protocol frames.  
**Deliverable:** Camera permission flow, CameraX preview, QR analyzer, frame validator, duplicate filter, receiver UI, and camera lifecycle cleanup.  
**Acceptance criteria:** Permission diminta hanya saat mode Terima dipilih; denial dan permanent denial memiliki recovery copy; preview berhenti ketika screen ditinggalkan; valid frame diterima; invalid protocol frame ditolak; duplicate frame tidak dihitung dua kali; camera test dilakukan pada perangkat Android nyata.  

### Issue 06 — [P0] Reconstruct, verify, and save received files

**Dependency:** Issue 02 dan Issue 05.  
**Tujuan:** Mengubah kumpulan frame menjadi file yang benar dan menyimpannya melalui system document picker.  
**Deliverable:** Sequential chunk reassembler, temporary file store, progress model, SHA-256 verifier, save file flow, filename sanitization, dan error UI.  
**Acceptance criteria:** File hasil identik secara byte dengan sumber pada golden fixtures; checksum mismatch mencegah save-success state; file kosong, file besar dalam batas MVP, Unicode filename, dan MIME type ditangani; partial session dapat dibatalkan dan dibersihkan; file tersimpan menggunakan nama yang aman.  

### Issue 07 — [P0] Add loss-tolerant fountain-code transfer

**Dependency:** Issue 04, Issue 05, dan Issue 06.  
**Tujuan:** Mengganti sequential-only behavior dengan adapter fountain code yang toleran terhadap frame hilang, duplikat, dan urutan acak.  
**Deliverable:** Fountain encoder, decoder, parameter contract, parity/seed frame format, deduplication strategy, dropped-frame tests, and migration note from sequential prototype.  
**Acceptance criteria:** Receiver dapat menyelesaikan file ketika frame diacak; transfer tetap selesai setelah frame tertentu dihilangkan; duplicate tidak mengurangi correctness; decoder hanya complete ketika blok cukup dan checksum cocok; memory growth berada dalam batas yang terdokumentasi.  

### Issue 08 — [P0] Add transfer state machine, cancellation, and recovery

**Dependency:** Issue 04, Issue 05, dan Issue 06.  
**Tujuan:** Menyatukan sender, receiver, storage, camera, dan UI melalui state machine yang eksplisit.  
**Deliverable:** `TransferState`, event model, reducer/ViewModel, cancellation rules, timeout/restart behavior, cleanup hooks, and user-facing error codes.  
**Acceptance criteria:** Semua state normal dan error utama memiliki transition yang terdokumentasi; cancel dari sender atau receiver membersihkan resource; rotate/recreate activity tidak menyebabkan crash; camera, coroutine, timer, and temp file cleanup diverifikasi; UI tidak menunjukkan completed sebelum verifier sukses.  

### Issue 09 — [P1] Add diagnostics and repeatable device benchmark

**Dependency:** Issue 04, Issue 05, Issue 07, dan Issue 08.  
**Tujuan:** Mengukur performa nyata dan menyediakan data untuk tuning QR payload serta frame interval.  
**Deliverable:** Diagnostics screen, event counters, benchmark fixture, CSV/JSON export lokal, test protocol, dan report template.  
**Acceptance criteria:** Sistem mencatat transfer duration, bytes delivered, decode success, duplicate count, dropped-frame estimate, frame interval, goodput, dan final checksum; benchmark dapat diulang pada pasangan perangkat; log tidak menyimpan isi file; hasil dapat dibandingkan antar konfigurasi.  

### Issue 10 — [P0/P1] Add privacy copy, accessibility, screen-on, and UX hardening

**Dependency:** Issue 04, Issue 05, dan Issue 08.  
**Tujuan:** Menjadikan alur dapat dipahami, aman secara ekspektasi, dan usable pada perangkat nyata.  
**Deliverable:** Privacy notice, no-network explanation, permission rationale, screen-on implementation, distance/brightness guidance, content descriptions, focus semantics, dark theme, landscape layout, and reduced-motion-friendly status treatment.  
**Acceptance criteria:** Pengguna diberi tahu bahwa transfer tidak terenkripsi pada MVP; UI tidak menjanjikan confidentiality; seluruh action utama dapat dipakai dengan screen reader; loading/error/completed state terbaca sebagai teks; layar tetap aktif saat sender berjalan; landscape dan dark mode tidak memotong kontrol utama.  

### Issue 11 — [P0] Perform two-device offline QA and release hardening

**Dependency:** Issue 07, Issue 08, Issue 09, dan Issue 10.  
**Tujuan:** Membuktikan aplikasi bekerja di luar emulator dan tanpa jalur jaringan.  
**Deliverable:** Device matrix, offline test report, known limitations, crash checklist, APK smoke-test checklist, and release candidate build.  
**Acceptance criteria:** Wi-Fi, Bluetooth, dan mobile data dapat dimatikan setelah aplikasi tersedia; minimal dua perangkat Android dapat menyelesaikan transfer; test mencakup gambar, PDF, audio, video kecil, biner, dropped frames, duplicate frames, rotation, low brightness, denied camera permission, cancel, checksum mismatch, dan app recreation; tidak ada blocker crash pada happy path.  

### Issue 12 — [P0] Document installation, AI Studio handoff, and known limitations

**Dependency:** Issue 11.  
**Tujuan:** Membuat hasil dapat dilanjutkan dari Google AI Studio ke Android Studio dan diuji oleh kontributor lain.  
**Deliverable:** README Android, AI Studio prompt, export/import guide, device test guide, protocol notes, troubleshooting, privacy statement, and release checklist.  
**Acceptance criteria:** Kontributor baru dapat membuat project dari prompt, mengunduh ZIP, membukanya di Android Studio, membangun APK, memasang ke dua perangkat, dan menjalankan smoke test; batasan emulator kamera, single-module, client-side-only, serta ZIP export terdokumentasi.[1]  

## Quality gate global

Issue tidak boleh ditutup hanya karena build berhasil. Setiap issue harus memiliki test atau bukti manual yang sesuai, tidak meninggalkan network dependency yang tidak direncanakan, tidak menulis isi file ke log, dan memperbarui TDD jika kontrak arsitektur atau protocol berubah.

## Referensi

[1]: https://ai.google.dev/gemini-api/docs/aistudio-android "Google AI Studio — Build Android Apps"
[2]: https://github.com/bashalarmistalt/decimen-optical-transfer/ "Decimen Optical Transfer — fountain-coded QR file transfer"
