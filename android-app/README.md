# Lumen Transfer — Android Native App

Lumen Transfer adalah aplikasi Android standalone untuk memindahkan file melalui layar pengirim dan kamera penerima. Implementasinya mengikuti batasan Google AI Studio Android: Kotlin, Jetpack Compose, single activity, single module, client-side only, tanpa backend atau NDK. Struktur dan kontraknya mengikuti [`../docs/OPTICAL-TRANSFER-PRD.md`](../docs/OPTICAL-TRANSFER-PRD.md), [`../docs/OPTICAL-TRANSFER-ISSUES.md`](../docs/OPTICAL-TRANSFER-ISSUES.md), dan [`../docs/OPTICAL-TRANSFER-TDD.md`](../docs/OPTICAL-TRANSFER-TDD.md).

## Status

Milestone #21 mencakup project Kotlin + Jetpack Compose yang telah divalidasi dengan `:app:testDebugUnitTest` dan `assembleDebug`, Home/Send/Receive vertical slice, system document picker, metadata dan SHA-256, permission kamera, immutable transfer state, versioned `FrameEnvelope` dengan CRC32, ZXing QR adapter, fountain sender/receiver, CameraX Preview + ImageAnalysis, reconstruction dan save flow, timeout/cancellation/lifecycle cleanup, serta diagnostics lokal tanpa payload logging. Pengguna dapat mengekspor snapshot diagnostics sender atau receiver sebagai JSON melalui Storage Access Framework. APK debug terbaru berada di `app/build/outputs/apk/debug/app-debug.apk` setelah build.

**Receiver CameraX, reconstruction, fountain redundancy, immutable state machine, dan diagnostics lokal sudah terintegrasi secara kode**, menggunakan `PreviewView`, `ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888`, `STRATEGY_KEEP_ONLY_LATEST`, executor tunggal, `ImageProxy.close()` pada `finally`, metadata decoder, bounded temporary file, systematic + repair frame, deterministic seed/index selection, incremental GF(2) decoder, SHA-256 verification, Unicode filename sanitization, `ACTION_CREATE_DOCUMENT` save flow, `TransferState` reducer, timeout watchdog, cancellation, rotation event, lifecycle cleanup, `TransferDiagnostics`, atomic diagnostics store, serta export JSON tanpa payload. Physical-device recovery-rate/lifecycle QA masih terbuka. Emulator berbasis browser tidak cukup untuk memvalidasi camera capture; acceptance akhir wajib dilakukan pada perangkat Android nyata. Keputusan fountain dicatat di [`../docs/ADR-FOUNTAIN-CODEC.md`](../docs/ADR-FOUNTAIN-CODEC.md).

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
├── app                       Android Window adapter
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

Envelope serializer, fountain transport, QR codec, CameraX analyzer, reconstruction, state machine, dan local diagnostics export sudah terintegrasi sampai milestone #21. Diagnostics hanya mencatat timing, frame counters, block counters, equation count, byte counters, dan goodput; payload file maupun isi QR tidak ditulis ke log atau export. Physical-device benchmark dan QA penerimaan tetap menjadi pekerjaan issue #23.

## Privacy

Aplikasi tidak memiliki backend dan transfer tidak membutuhkan Wi-Fi, Bluetooth, mobile data, atau network request. Diagnostics export ditulis hanya ke URI lokal yang dipilih pengguna dan tidak memuat payload file, nama isi file, atau data rahasia. Checksum memvalidasi integritas, bukan kerahasiaan. QR yang tampil pada layar dapat dibaca oleh kamera lain; enkripsi passphrase adalah pekerjaan P1 dan belum diimplementasikan.
