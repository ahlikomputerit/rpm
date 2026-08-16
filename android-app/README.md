# Lumen Transfer — Android Native Skeleton

Lumen Transfer adalah aplikasi Android standalone untuk memindahkan file melalui layar pengirim dan kamera penerima. Project ini adalah fallback implementasi native setelah generator Google AI Studio mengalami internal error. Struktur dan kontraknya mengikuti [`../docs/OPTICAL-TRANSFER-PRD.md`](../docs/OPTICAL-TRANSFER-PRD.md), [`../docs/OPTICAL-TRANSFER-ISSUES.md`](../docs/OPTICAL-TRANSFER-ISSUES.md), dan [`../docs/OPTICAL-TRANSFER-TDD.md`](../docs/OPTICAL-TRANSFER-TDD.md).

## Status

Checkpoint ini mencakup project Kotlin + Jetpack Compose yang telah divalidasi dengan `:app:testDebugUnitTest` dan `assembleDebug`, Home/Send/Receive vertical slice, system document picker, metadata dan SHA-256, permission kamera, state ViewModel, kontrak `FrameEnvelope`, CRC32, serializer/parser, sequential frame source, ZXing QR encoder/decoder adapter, QR preview Compose, pause/resume/cancel sender loop, CameraX Preview + ImageAnalysis, QR image analyzer, parser bridge, deduplication, transfer-ID validation, dan unit test protocol/QR/receiver. APK debug terbaru berada di `app/build/outputs/apk/debug/app-debug.apk` setelah build.

**Receiver CameraX sudah terintegrasi secara kode**, menggunakan `PreviewView`, `ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888`, `STRATEGY_KEEP_ONLY_LATEST`, executor tunggal, `ImageProxy.close()` pada `finally`, dan unbind saat lifecycle berhenti. Rekonstruksi file, fountain-code redundancy, dan physical-device QA masih terbuka. Emulator berbasis browser tidak cukup untuk memvalidasi camera capture; acceptance akhir wajib dilakukan pada perangkat Android nyata. Keputusan library dicatat di [`../docs/ADR-QR-CODEC.md`](../docs/ADR-QR-CODEC.md).

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
├── domain.protocol           Wire constants and binary serializer/parser
├── domain.integrity          CRC32 and SHA-256
├── domain.runtime             ScreenOnPolicy interface
├── data.file                 Storage Access Framework adapter
├── presentation.home         Home screen
├── presentation.send         SendViewModel and picker UI
└── presentation.receive      ReceiveViewModel and camera permission UI
```

## Kontrak protocol checkpoint

Wire envelope dimulai dengan magic `LT`, protocol version, flags, 16-byte transfer ID, frame kind, seed, degree, sequence, payload length, payload, dan CRC32. Payload maksimum sementara adalah 1024 byte per frame dan batas file MVP adalah 10 MB. Parser menolak magic invalid, version yang tidak didukung, frame terpotong, payload length tidak konsisten, dan CRC mismatch.

Serializer ini belum menjadi fountain-code transport. Sequential chunk adapter, QR encoder/decoder, CameraX `ImageAnalysis`, fountain-code decoder, reconstruction, dan device benchmark dikerjakan sesuai urutan issue GitHub #15–#23.

## Privacy

Aplikasi tidak memiliki backend dan checkpoint ini tidak melakukan network request untuk transfer. Checksum memvalidasi integritas, bukan kerahasiaan. QR yang tampil pada layar dapat dibaca oleh kamera lain; enkripsi passphrase adalah pekerjaan P1 dan belum diimplementasikan.
