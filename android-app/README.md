# Lumen Transfer — Android Native App

Lumen Transfer adalah aplikasi Android standalone untuk memindahkan file melalui layar pengirim dan kamera penerima. Implementasinya mengikuti batasan Google AI Studio Android: Kotlin, Jetpack Compose, single activity, single module, client-side only, tanpa backend atau NDK. Struktur dan kontraknya mengikuti [`../docs/OPTICAL-TRANSFER-PRD.md`](../docs/OPTICAL-TRANSFER-PRD.md), [`../docs/OPTICAL-TRANSFER-ISSUES.md`](../docs/OPTICAL-TRANSFER-ISSUES.md), dan [`../docs/OPTICAL-TRANSFER-TDD.md`](../docs/OPTICAL-TRANSFER-TDD.md).

## Status

Milestone #22 mencakup seluruh milestone #21 ditambah privacy notice sebelum transfer pertama, penyimpanan persetujuan lokal melalui SharedPreferences, Material 3 light/dark color scheme, dukungan layout landscape melalui scroll container, screen-reader semantics dan live-region announcements, panduan brightness/jarak 15–30 cm, accessible permission/checksum/reconstruction errors, serta lifecycle cleanup untuk pause sender dan unbind camera saat host berhenti. Validasi terbaru berhasil dengan `:app:testDebugUnitTest`; assembleDebug tetap menjadi gate release pada setiap checkpoint. APK debug berada di `app/build/outputs/apk/debug/app-debug.apk` setelah build.

**Receiver CameraX, reconstruction, fountain redundancy, immutable state machine, diagnostics lokal, dan UX hardening sudah terintegrasi secara kode**, menggunakan `PreviewView`, `ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888` dengan Y-plane luminance langsung, `STRATEGY_KEEP_ONLY_LATEST`, executor tunggal, `ImageProxy.close()` pada `finally`, metadata decoder, bounded temporary file, systematic + repair frame, deterministic seed/index selection, incremental GF(2) decoder, SHA-256 verification, Unicode filename sanitization, `ACTION_CREATE_DOCUMENT` save flow, `TransferState` reducer, timeout watchdog, cancellation, rotation event, lifecycle cleanup, `TransferDiagnostics`, atomic diagnostics store, JSON export tanpa payload, privacy notice, dark theme, landscape scroll, screen-reader semantics, live regions, brightness/distance guidance, accessible error copy, dan live QR detector normal untuk full camera preview dengan center-ROI/binarizer fallback. Physical-device recovery-rate/lifecycle QA masih terbuka. Diagnosis kegagalan uji perangkat dan prosedur re-test dicatat di [`../docs/OPTICAL-TRANSFER-DEVICE-DIAGNOSIS.md`](../docs/OPTICAL-TRANSFER-DEVICE-DIAGNOSIS.md). Emulator berbasis browser tidak cukup untuk memvalidasi camera capture; acceptance akhir wajib dilakukan pada perangkat Android nyata. Keputusan fountain dicatat di [`../docs/ADR-FOUNTAIN-CODEC.md`](../docs/ADR-FOUNTAIN-CODEC.md).

## Membuka project

Buka folder `android-app/` menggunakan Android Studio versi yang mendukung Android Gradle Plugin 8.7.x dan Kotlin 2.0.x. Pastikan Android SDK Platform 35 tersedia. Jika membuka melalui Google AI Studio, gunakan ZIP export dari project Android dan salin/terapkan source checkpoint ini pada project tersebut.

Perintah lokal setelah Android SDK dan Gradle tersedia:

```bash
./gradlew test
./gradlew assembleDebug
```

Jika repository belum memiliki Gradle wrapper, Android Studio dapat membuat atau memperbarui wrapper melalui **Gradle > Wrapper**. Jangan commit `local.properties` karena path SDK bersifat lokal.

## Package map

```text
com.ahlikomputerit.lumentransfer
├── app                       Android Window adapter and privacy notice
├── domain.model              TransferId, FileMetadata, FrameEnvelope, states
├── domain.protocol           Wire constants, fountain codec, serializer/parser
├── domain.diagnostics        Payload-free timing, counters, and throughput metrics
├── domain.integrity          CRC32 and SHA-256
├── domain.runtime             ScreenOnPolicy interface
├── data.file                 Storage Access Framework and diagnostics writer adapters
├── presentation.home         Home screen
├── presentation.send         SendViewModel and picker UI
└── presentation.receive      ReceiveViewModel and camera permission UI
```

## Kontrak protocol checkpoint

Wire envelope dimulai dengan magic `LT`, protocol version, flags, 16-byte transfer ID, frame kind, seed, degree, sequence, payload length, payload, dan CRC32. Payload maksimum sementara adalah 1024 byte per frame dan batas file MVP adalah 10 MB. Parser menolak magic invalid, version yang tidak didukung, frame terpotong, payload length tidak konsisten, dan CRC mismatch.

Envelope serializer, fountain transport, QR codec, CameraX analyzer, reconstruction, state machine, local diagnostics export, dan UX hardening sudah terintegrasi sampai milestone #22. Setelah tiga rangkaian uji nyata menunjukkan 0 accepted frame, ditemukan bahwa decoder salah membaca urutan byte CameraX `RGBA_8888` sebagai A,R,G,B, padahal urutannya R,G,B,A. Perbaikan berikutnya memindahkan jalur produksi ke Y-plane luminance `YUV_420_888`, sehingga decoder tidak lagi bergantung pada konversi RGBA; RGBA tetap diuji sebagai compatibility path. Decoder memakai full-preview, center-ROI dan binarizer fallback; sender menggunakan block size 256 byte serta QR quiet zone margin 4, renderer memakai batas pixel integer, dan receiver memiliki guard terhadap double-bind CameraX. Diagnostics sekarang memisahkan rejection protocol serta telemetry camera berupa frame count, resolusi, stride, rotasi, luminance range, dan QR module count tanpa menyimpan gambar atau payload. Physical-device re-test diperlukan untuk memverifikasi perbaikan pada issue #23.

## Privacy

Aplikasi tidak memiliki backend dan transfer tidak membutuhkan Wi-Fi, Bluetooth, mobile data, atau network request. Privacy notice ditampilkan sebelum transfer pertama dan persetujuannya disimpan lokal pada perangkat; tidak ada data persetujuan yang dikirim keluar. Diagnostics export ditulis hanya ke URI lokal yang dipilih pengguna dan tidak memuat payload file, isi file, atau data rahasia. Checksum memvalidasi integritas, bukan kerahasiaan. QR yang tampil pada layar dapat dibaca oleh kamera lain; enkripsi passphrase adalah pekerjaan P1 dan belum diimplementasikan.
