# Lumen Transfer — Technical Design Document

**Status:** Draft implementable  
**Versi:** 1.0  
**PRD:** [`docs/OPTICAL-TRANSFER-PRD.md`](./OPTICAL-TRANSFER-PRD.md)  
**Issue blueprint:** [`docs/OPTICAL-TRANSFER-ISSUES.md`](./OPTICAL-TRANSFER-ISSUES.md)  
**Target:** Android native standalone  
**Implementasi awal:** Kotlin + Jetpack Compose melalui Google AI Studio Build mode  
**Tanggal:** 2026-08-16  

## 1. Tujuan teknis

Dokumen ini menerjemahkan PRD Lumen Transfer menjadi rancangan yang dapat diimplementasikan secara bertahap. Rancangan harus menghasilkan aplikasi Android yang dapat mengirim satu file melalui layar sebagai animasi QR dan menerima file melalui kamera, tanpa jaringan atau backend. Rancangan juga harus tetap berada dalam batasan Google AI Studio Android: Kotlin, Jetpack Compose, Gradle Kotlin DSL, single activity, single module, client-side only, tanpa Java/XML layout dan tanpa NDK/native C++.[1]

TDD ini sengaja memisahkan **domain protocol** dari UI, kamera, dan file system. Dengan begitu, encoder/decoder dapat diuji di JVM tanpa kamera, sementara pipeline kamera dapat diuji menggunakan fixture frame dan perangkat nyata secara terpisah.

## 2. Keputusan arsitektur

| Keputusan | Pilihan | Alasan |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Sesuai output Android Google AI Studio dan mudah dipisahkan dari domain state.[1] |
| Struktur app | Single activity, single module | Batasan saat ini pada Android Build mode AI Studio.[1] |
| State | ViewModel + immutable UI state + event reducer | Menjaga lifecycle dan rotasi tidak merusak sesi. |
| File input | Storage Access Framework `ACTION_OPEN_DOCUMENT` | Pengguna memilih URI tanpa permission storage luas.[3] |
| File output | Storage Access Framework `ACTION_CREATE_DOCUMENT` | Pengguna memilih lokasi dan nama hasil secara eksplisit.[3] |
| Camera | CameraX `Preview` + `ImageAnalysis` `1.4.1` | Memberi pipeline frame CPU-accessible dan lifecycle-aware.[2] |
| Backpressure | `STRATEGY_KEEP_ONLY_LATEST` | Frame QR lama lebih baik dibuang daripada membuat queue kamera menumpuk.[2] |
| QR codec | JVM/Kotlin-compatible encoder/decoder | AI Studio Android tidak mendukung NDK/native C++.[1] |
| Transfer | Sender screen → receiver camera, no back-channel | Sesuai tujuan air-gapped optical transfer. |
| Integrity | SHA-256 file + CRC32/frame validation | SHA-256 memvalidasi hasil akhir; CRC32 menolak frame rusak lebih awal. |
| Temporary data | App-specific cache/files directory | Tidak membutuhkan akses storage umum dan mudah dibersihkan. |
| Network | Tidak ada dependency runtime | File tidak keluar dari perangkat; build tidak menambahkan server. |

### 2.1 Batasan platform yang memengaruhi desain

Browser-based Android emulator di Google AI Studio tidak mendukung camera dan photo capture. Karena itu, emulator hanya dipakai untuk memvalidasi navigasi, UI, state dummy, dan sebagian logic; seluruh alur receiver wajib diuji pada perangkat Android fisik.[1]

Project Android AI Studio dapat diunduh sebagai ZIP untuk dilanjutkan di Android Studio. TDD ini tidak mengasumsikan export GitHub langsung dari Android project; PRD, issue, dan dokumen desain disimpan di repositori terpisah agar proses handoff tetap terlacak.[1]

## 3. Struktur package

Walaupun AI Studio membuat single module, package harus dipisahkan berdasarkan tanggung jawab.

```text
app/src/main/java/<package>/
├── MainActivity.kt
├── app/
│   ├── AppContainer.kt
│   ├── AppNavHost.kt
│   └── AppTheme.kt
├── domain/
│   ├── model/
│   │   ├── FileMetadata.kt
│   │   ├── FrameEnvelope.kt
│   │   ├── TransferId.kt
│   │   ├── TransferState.kt
│   │   └── TransferError.kt
│   ├── protocol/
│   │   ├── FrameSerializer.kt
│   │   ├── FrameParser.kt
│   │   ├── Chunker.kt
│   │   ├── FountainEncoder.kt
│   │   ├── FountainDecoder.kt
│   │   └── ProtocolConstants.kt
│   ├── integrity/
│   │   ├── Sha256Hasher.kt
│   │   └── Crc32.kt
│   └── repository/
│       ├── TransferRepository.kt
│       └── TemporaryFileRepository.kt
├── data/
│   ├── file/
│   │   ├── FilePicker.kt
│   │   ├── ContentResolverFileReader.kt
│   │   └── DocumentSaver.kt
│   ├── camera/
│   │   ├── CameraController.kt
│   │   ├── QrAnalyzer.kt
│   │   └── QrDecoder.kt
│   └── qr/
│       ├── QrEncoder.kt
│       └── QrFrameRenderer.kt
├── presentation/
│   ├── home/HomeScreen.kt
│   ├── send/SendScreen.kt
│   ├── send/SendViewModel.kt
│   ├── receive/ReceiveScreen.kt
│   ├── receive/ReceiveViewModel.kt
│   ├── diagnostics/DiagnosticsScreen.kt
│   └── components/
└── testing/
    ├── FakeQrDecoder.kt
    ├── FakeClock.kt
    └── TestFixtures.kt
```

Package tidak boleh membuat domain bergantung pada Compose, CameraX, Android `Context`, atau `Uri`. Adapter Android berada di `data/`, sedangkan `domain/` menerima `InputStream`, `OutputStream`, byte array, atau interface yang dapat dipalsukan.

## 4. Domain model

Model inti harus immutable. Contoh kontrak Kotlin berikut bersifat normatif; nama dapat disesuaikan selama makna dan invariannya tetap.

```kotlin
data class TransferId(val value: ByteArray) {
    init { require(value.size == 16) }
}

data class FileMetadata(
    val transferId: TransferId,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: ByteArray,
    val blockSize: Int,
    val sourceBlockCount: Int,
)

enum class FrameKind {
    META,
    SYSTEMATIC_DATA,
    REPAIR_DATA,
    END,
}

data class FrameEnvelope(
    val version: Int,
    val flags: Int,
    val transferId: TransferId,
    val kind: FrameKind,
    val seed: Long,
    val degree: Int,
    val payload: ByteArray,
    val frameCrc32: UInt,
)

sealed interface TransferState {
    data object Idle : TransferState
    data object Preparing : TransferState
    data class Sending(val sentFrames: Long, val emittedBytes: Long) : TransferState
    data class Scanning(val uniqueFrames: Long) : TransferState
    data class Decoding(val recoveredBlocks: Int, val totalBlocks: Int) : TransferState
    data object Verifying : TransferState
    data class Completed(val outputName: String, val sizeBytes: Long) : TransferState
    data class Cancelled(val reason: CancelReason) : TransferState
    data class Failed(val error: TransferError) : TransferState
}
```

`ByteArray` yang diterima dari decoder harus disalin atau dimiliki secara jelas. Decoder tidak boleh menyimpan referensi ke buffer kamera yang lifecycle-nya telah ditutup. Metadata wajib menolak ukuran negatif, nama kosong setelah sanitasi, checksum dengan panjang salah, block size nol, dan block count yang tidak sesuai dengan ukuran file.

## 5. Wire protocol

### 5.1 Prinsip

Wire format harus biner, berversi, memiliki magic prefix, dapat ditolak sebelum parsing mahal, dan tidak mengandung secret atau data logis yang tidak dibutuhkan receiver. Frame tidak boleh bergantung pada urutan kedatangan. Metadata yang sama boleh muncul ulang karena sender tidak memiliki back-channel.

MVP dapat memakai sequential chunk adapter untuk memvalidasi seluruh jalur. Format frame tetap harus menyediakan `seed`, `degree`, dan `frameKind` agar fountain-code adapter dapat diperkenalkan tanpa mengganti envelope utama.

### 5.2 Layout envelope

Semua integer menggunakan **unsigned big-endian** kecuali dinyatakan lain. Panjang field tetap mencegah parser ambigu.

| Offset | Ukuran | Field | Keterangan |
|---:|---:|---|---|
| 0 | 2 | `magic` | Nilai ASCII `LT`, yaitu `0x4C 0x54`. |
| 2 | 1 | `version` | Dimulai dari `1`. Versi yang tidak dikenal ditolak. |
| 3 | 1 | `flags` | Bit compression/encryption/reserved. |
| 4 | 16 | `transferId` | Random per sesi, tidak berasal dari nama file. |
| 20 | 1 | `frameKind` | META, SYSTEMATIC_DATA, REPAIR_DATA, END. |
| 21 | 4 | `seed` | Seed deterministik untuk pemilihan blok. |
| 25 | 2 | `degree` | Jumlah source block yang di-XOR; `0` untuk metadata. |
| 27 | 4 | `sequence` | Urutan diagnostik, bukan correctness requirement. |
| 31 | 4 | `payloadLength` | Panjang payload. Dibatasi oleh QR budget. |
| 35 | N | `payload` | Metadata atau encoded block payload. |
| 35+N | 4 | `frameCrc32` | CRC32 atas header dan payload sebelum CRC. |

Frame metadata payload menggunakan struktur length-prefixed UTF-8 untuk `fileName` dan `mimeType`, diikuti ukuran file, block size, source block count, serta SHA-256. Filename harus dibatasi panjangnya dan disanitasi hanya pada saat save; nama asli boleh dipertahankan sebagai metadata untuk tampilan.

### 5.3 QR transport encoding

`FrameSerializer` menghasilkan `ByteArray`. `QrEncoder` menerima adapter `ByteArray -> QrMatrix` atau `ByteArray -> String`, tergantung library yang dipilih pada Issue 03. Jika library hanya menerima string, gunakan Base64 URL-safe tanpa newline sebagai adapter sementara dan ukur overhead. Jangan menjadikan Base64 sebagai kontrak domain.

QR version, correction level, margin, dan payload budget harus disentralisasi di `ProtocolConstants`. Default awal bersifat konservatif. Tuning hanya boleh mengubah konfigurasi, bukan format envelope.

### 5.4 Sender scheduling

Urutan pengiriman awal adalah satu frame META, systematic data frames, repair frames, lalu END frame. Semua frame diulang dalam loop. Receiver tidak mengirim acknowledgement, sehingga sender harus terus memancarkan sampai pengguna menekan stop atau timeout kebijakan tercapai.

Scheduler harus memakai monotonic clock. `delay()` tidak boleh dijalankan pada main thread. Ketika lifecycle sender tidak resumed, loop harus berhenti atau dipause sesuai policy dan tidak terus mempertahankan resource layar yang sudah tidak terlihat.

## 6. Fountain-code design

### 6.1 Source blocks

File dibaca dalam block berukuran tetap, misalnya 512–2048 byte pada eksperimen awal. `sourceBlockCount = ceil(sizeBytes / blockSize)`. Block terakhir dipadding nol; panjang file asli berada di metadata sehingga padding tidak masuk hasil akhir.

### 6.2 Systematic frames

Systematic frame mengirim source block asli dengan `degree = 1` dan seed yang mengidentifikasi index blok. Frame ini mempercepat decoder dan memudahkan debugging. Sender tidak boleh hanya mengirim systematic frame pada production mode karena frame hilang dapat membuat receiver stall.

### 6.3 Repair frames

Repair frame menghasilkan himpunan index block dari `seed` dan degree distribution yang deterministic. Payload adalah XOR block-block yang dipilih. Receiver menyimpan persamaan `(selectedBlocks, payload)` dan menjalankan peeling ketika satu block diketahui.

Implementasi pertama harus menggunakan sistematis + repair frame, golden vectors deterministic, serta batas jumlah persamaan untuk mencegah memory growth tak terbatas. Jika peeling stall pada target loss rate, decoder harus memiliki inactivation/linear-solve fallback untuk himpunan kecil atau issue terpisah harus mendokumentasikan bahwa target tersebut belum tercapai.

### 6.4 Completion condition

Decoder complete hanya jika semua source block dapat dipulihkan dan hasil byte yang dipotong ke `sizeBytes` memiliki SHA-256 sama dengan metadata. Jumlah frame yang diterima tidak pernah menjadi completion condition tunggal.

## 7. Sender pipeline

```text
OpenDocument URI
  → ContentResolver metadata
  → streaming SHA-256 + source block reader
  → FileMetadata
  → FrameSerializer(META)
  → systematic/fountain encoder
  → FrameSerializer(DATA)
  → QrEncoder
  → Compose QR canvas/image
  → timed loop with screen-on
```

`ContentResolver` harus dibaca secara streaming. Sender tidak perlu memuat seluruh file ke RAM. Untuk MVP 10 MB, memory implementation masih boleh memakai buffer bounded, tetapi interface harus berupa `InputStream`/block reader agar limit dapat dinaikkan.

File picker menggunakan `ACTION_OPEN_DOCUMENT`, yang memberi URI terpilih pengguna dan tidak memerlukan permission storage umum.[3] Sender harus menangani URI yang tidak dapat dibaca, MIME type kosong, ukuran tidak tersedia, serta file berubah atau akses dicabut ketika transfer dimulai.

Screen-on hanya aktif selama send flow dan harus dilepas pada `onStop`, cancel, completed, serta error. Sender tidak boleh memakai wakelock atau foreground service pada MVP.

## 8. Receiver camera pipeline

```text
CameraX Preview + ImageAnalysis
  → latest-frame backpressure
  → QrAnalyzer.analyze(ImageProxy)
  → QrDecoder.decode(frame)
  → copy ByteArray
  → FrameParser
  → CRC/version/transfer validation
  → deduplication
  → protocol decoder
  → ReceiveViewModel state
```

CameraX `ImageAnalysis` memberikan frame CPU-accessible melalui `ImageAnalysis.Analyzer`; analyzer harus cepat dan memanggil `ImageProxy.close()` setelah selesai.[2] Gunakan `STRATEGY_KEEP_ONLY_LATEST` agar frame lama yang tidak lagi berguna dibuang ketika decoder tertinggal. Decode tidak boleh menjalankan operasi blocking panjang di main thread.

Analyzer harus memisahkan tiga langkah: ekstraksi frame kamera, QR decode, dan pengiriman hasil ke domain. `ImageProxy` harus ditutup dalam `finally`. Hasil decode harus disalin sebelum analyzer mengembalikan buffer ke CameraX.

Receiver mengabaikan QR yang bukan magic `LT`, versi tidak didukung, transfer ID berbeda dari sesi aktif, CRC gagal, payload length invalid, atau frame duplicate. Invalid frame tidak boleh menjadi error fatal; diagnostics boleh menghitungnya sebagai rejected frame.

### 8.1 Permission lifecycle

Permission kamera diminta ketika pengguna masuk ke Receive screen, bukan saat first launch. Jika denied, tampilkan rationale dan tombol retry. Jika permanently denied, arahkan ke system settings tanpa membuka kamera secara paksa. Ketika composable hilang atau lifecycle berhenti, unbind CameraX dan close analyzer executor.

## 9. Reconstruction dan storage

Receiver menggunakan temporary file di app-specific storage. Decoder menulis block yang telah diketahui ke struktur bounded. Untuk sequential adapter, tulis berdasarkan index. Untuk fountain decoder, simpan source blocks dan equations secara terkontrol lalu stream hasil akhir ke temp file ketika complete.

Setelah decoder complete, hitung SHA-256 dari temp file. Jika cocok, tampilkan Completed dan panggil `ACTION_CREATE_DOCUMENT` ketika pengguna menekan Save. Jika mismatch, ubah state menjadi Failed dengan error `IntegrityMismatch`, hapus temp file, dan jangan memanggil save picker.

Storage Access Framework mendukung pemilihan file sumber melalui `ACTION_OPEN_DOCUMENT` dan pembuatan file melalui `ACTION_CREATE_DOCUMENT`; app memperoleh akses melalui URI yang dipilih pengguna.[3] Receiver tidak boleh mengasumsikan URI memiliki filesystem path yang langsung dapat dibuka dengan `File()`.

## 10. State machine

| Current state | Event | Next state | Side effect |
|---|---|---|---|
| Idle | SelectSend | Preparing | Buka picker. |
| Idle | SelectReceive | Scanning | Minta camera permission. |
| Preparing | FileReady | Sending | Hitung metadata/checksum dan mulai scheduler. |
| Preparing | FileError | Failed | Tampilkan retry. |
| Sending | Pause | Sending paused variant | Hentikan timer, pertahankan session. |
| Sending | Cancel | Cancelled | Stop scheduler, release screen-on, cleanup. |
| Scanning | ValidMeta | Scanning | Set active transfer metadata. |
| Scanning | ValidData | Decoding | Dedup dan pass ke decoder. |
| Decoding | MoreFrame | Decoding | Update progress. |
| Decoding | Complete | Verifying | Tulis/read hasil untuk SHA-256. |
| Verifying | HashMatch | Completed | Tawarkan save. |
| Verifying | HashMismatch | Failed | Delete temp file, tampilkan error. |
| Any active | LifecycleStop | Paused/Cancelled by policy | Release camera/timer; policy terdokumentasi. |

State harus serializable atau reconstructable enough untuk configuration change. MVP tidak menjanjikan resume setelah process death. Jika Activity recreation terjadi, ViewModel mempertahankan state selama proses hidup; camera binding dibuat ulang secara idempotent.

## 11. Threading dan lifecycle

| Work | Thread/context |
|---|---|
| Compose rendering | Main/UI thread. |
| File metadata and SHA-256 | `Dispatchers.IO`. |
| QR encode | Dedicated bounded executor atau `Dispatchers.Default`. |
| Camera image analysis | CameraX analyzer executor; jangan blocking main. |
| Protocol parse and CRC | `Dispatchers.Default` atau analyzer executor untuk pekerjaan kecil. |
| Fountain decode | `Dispatchers.Default`, dengan cancellation checks. |
| Temp file read/write | `Dispatchers.IO`. |
| UI state updates | Main-safe `StateFlow` collected by Compose. |

Semua long-running loop harus cooperative-cancellable. `finally` wajib menutup input stream, output stream, ImageProxy, executor, coroutine job, camera binding, dan screen-on flag. Tidak boleh ada `GlobalScope`.

## 12. Error taxonomy

Error code harus stabil dan dapat dilokalkan tanpa menampilkan stack trace kepada pengguna.

| Code | Kondisi | User action |
|---|---|---|
| `CAMERA_PERMISSION_DENIED` | Kamera tidak diizinkan. | Retry atau buka settings. |
| `CAMERA_UNAVAILABLE` | Kamera sedang dipakai atau tidak tersedia. | Tutup aplikasi lain atau ganti perangkat. |
| `UNSUPPORTED_PROTOCOL` | Version/frame kind tidak didukung. | Pastikan kedua aplikasi versi kompatibel. |
| `FRAME_CORRUPT` | CRC atau payload invalid. | Tidak perlu action; sender mengulang frame. |
| `TRANSFER_ID_MISMATCH` | Frame berasal dari sesi lain. | Arahkan kamera ke layar pengirim yang benar. |
| `DECODER_STALLED` | Persamaan belum cukup untuk recovery. | Pertahankan posisi kamera dan biarkan sender mengulang. |
| `FILE_TOO_LARGE` | Melebihi batas MVP. | Gunakan file lebih kecil atau milestone berikutnya. |
| `INTEGRITY_MISMATCH` | SHA-256 hasil berbeda. | Buang hasil dan ulangi transfer. |
| `STORAGE_WRITE_FAILED` | URI output tidak dapat ditulis. | Pilih lokasi lain. |
| `SESSION_CANCELLED` | Pengguna membatalkan. | Mulai sesi baru jika diperlukan. |

Log debug hanya boleh mencatat code, state, byte count, frame count, dan timing. Nama file boleh disamarkan; payload dan isi file tidak boleh dicatat.

## 13. Dependency strategy

Dependency harus dijaga kecil dan kompatibel dengan AI Studio. AI Studio dapat mengelola Gradle dependencies dari Maven/Google repositories, tetapi setiap dependency QR dan kamera tetap harus diverifikasi pada perangkat nyata.[1]

| Kebutuhan | Kandidat | Keputusan |
|---|---|---|
| UI | Jetpack Compose, Material 3 | Wajib. |
| Lifecycle/state | AndroidX Lifecycle, ViewModel, Kotlin coroutines | Wajib. |
| Camera | CameraX Preview + ImageAnalysis `1.4.1` | Terintegrasi pada receiver; physical-device QA masih terbuka. |
| File picker | AndroidX Activity Result Contracts atau intents SAF | Wajib. |
| QR encoder/decoder | `com.google.zxing:core:3.5.4` melalui `QrEncoder`/`QrDecoder` | Dipilih pada spike Issue #15; provisional sampai physical-device benchmark. ADR: [`docs/ADR-QR-CODEC.md`](./ADR-QR-CODEC.md). |
| Hash | `MessageDigest` SHA-256 dari platform | Hindari dependency tambahan. |
| Serialization | Custom binary serializer atau minimal Kotlin serialization | Custom binary lebih terkontrol untuk QR budget. |
| Persistence | App-specific files + optional DataStore untuk settings | Database tidak diperlukan untuk MVP. |

Jika decoder QR terbaik membutuhkan native code, jangan menambahkannya diam-diam. Buat ADR baru, uji apakah library JVM cukup, dan bila tidak, pindahkan keputusan tersebut ke Android Studio/native implementation milestone di luar batas AI Studio.

## 14. Testing strategy

### 14.1 Unit test JVM

Unit test mencakup serializer round-trip, parser rejection, CRC, SHA-256 fixture, metadata validation, filename sanitization, sequential chunking, fountain deterministic seed, degree distribution, XOR operations, peeling decoder, duplicate filter, state reducer, dan error mapping.

Golden vectors harus memuat metadata frame, data frame, repair frame, frame dengan CRC invalid, versi unsupported, payload truncated, dan Unicode filename. Golden vector tidak boleh bergantung pada device, locale, atau waktu sistem.

### 14.2 Integration test tanpa kamera

Gunakan `FakeQrEncoder` dan `FakeQrDecoder` untuk menghubungkan sender scheduler dengan receiver decoder in-process. Test harus dapat mengacak frame, menghilangkan persentase frame tertentu, menduplikasi frame, menyisipkan noise frame, dan menghentikan sesi di tengah jalan.

### 14.3 Instrumentation test

Instrumented test mencakup Compose navigation, permission states, lifecycle, document picker result handling, temp storage cleanup, rotation, dark mode, accessibility semantics, dan save flow. CameraX pipeline dapat memakai fake analyzer untuk sebagian test, tetapi tidak menggantikan device test.

### 14.4 Physical-device test

Perangkat nyata wajib menguji camera focus, orientation, brightness, screen refresh, QR size, frame interval, thermal behavior, decoder throughput, dan lifecycle. Karena emulator AI Studio tidak mendukung camera capture, setiap perubahan pada `QrAnalyzer`, `CameraController`, atau QR dependency harus memiliki bukti device test.[1]

### 14.5 Acceptance matrix

| Skenario | Expected result |
|---|---|
| Gambar 100 KB | Selesai, hash cocok, save berhasil. |
| PDF Unicode filename | Nama aman, MIME dipulihkan, hash cocok. |
| File biner dengan byte nol | Byte output sama persis. |
| Frame acak | Fountain decoder tetap complete. |
| Frame hilang | Sender mengulang; transfer tidak corrupt. |
| Frame duplicate | Progress unique frame tidak terinflasi. |
| CRC invalid | Frame ditolak, sesi tetap berjalan. |
| Camera denied | UI memberi retry/settings path. |
| Rotate receiver | Camera rebind; state sesi konsisten. |
| Cancel sender | Timer berhenti dan screen-on dilepas. |
| Checksum mismatch | Save disabled; temp file dibuang. |
| Radio dimatikan | Transfer tetap berjalan setelah aplikasi siap. |

## 15. Performance budget awal

MVP harus menggunakan budget yang dapat diukur, bukan klaim kecepatan tetap. Target awal yang direkomendasikan adalah analyzer tidak menahan satu `ImageProxy` lebih lama dari satu interval frame kamera pada kondisi normal, UI frame tidak drop parah karena decode, dan memory temp buffer tetap bounded untuk file 10 MB.

Diagnostics mencatat `transferDurationMs`, `fileBytes`, `uniqueFrames`, `decodedFrames`, `rejectedFrames`, `duplicateFrames`, `frameIntervalMs`, `sourceBlockCount`, `recoveredBlockCount`, `goodputBytesPerSecond`, dan `sha256Match`. Tidak ada nilai diagnostik yang boleh berisi payload atau secret.

## 16. Security and privacy review

MVP bukan encrypted transport. QR yang sedang tampil adalah representasi data yang dapat dibaca kamera lain. Privacy copy harus muncul sebelum transfer pertama dan ringkasannya tersedia pada settings/about. SHA-256 hanya memeriksa integritas, bukan kerahasiaan.

P1 encryption dapat menambahkan AES-GCM. Kunci diturunkan dari passphrase manual menggunakan KDF yang tersedia dan parameter yang terdokumentasi. Nonce harus unik per transfer, authentication tag harus diverifikasi sebelum reconstruction complete, dan passphrase tidak boleh muncul di log atau disimpan tanpa persetujuan eksplisit. Enkripsi tidak boleh dianggap selesai hanya karena payload berubah menjadi ciphertext; threat model dan key handling harus diuji terpisah.

## 17. Implementation sequence

Urutan implementasi mengikuti issue yang telah dibuat di GitHub, yaitu #13 sampai #24 pada repositori `ahlikomputerit/rpm`.

| Fase | Issue | Fokus |
|---|---:|---|
| Foundation | #13 | Project, theme, navigation, build. |
| Protocol | #14–#15 | Envelope, checksum, QR dependency spike. |
| Vertical slice | #16–#18 | Sequential sender → camera receiver → reconstructed file. |
| Reliability | #19–#20 | Fountain code, state machine, cancellation. |
| Hardening | #21–#22 | Diagnostics, accessibility, privacy, screen-on. |
| Release | #23–#24 | Physical device QA, documentation, AI Studio handoff. |

Setiap fase harus menghasilkan software yang dapat dikompilasi. Jangan menggabungkan seluruh encoder, camera pipeline, fountain decoder, dan UI dalam satu prompt besar tanpa checkpoint karena kegagalan dependency atau lifecycle akan sulit diisolasi.

## 18. Prompt master untuk Google AI Studio

Prompt berikut dapat ditempel pada Google AI Studio Build mode setelah memilih platform Android. Prompt ini meminta skeleton dan vertical slice, bukan mengklaim seluruh production feature selesai dalam satu langkah.

```text
Build a native Android phone/tablet app named Lumen Transfer using Kotlin, Jetpack Compose, Material 3, a single activity, and a single Gradle module. The app is client-side only and must not use a backend, Firebase, network calls, login, cloud storage, or NDK/native C++.

Product goal:
Transfer one file from one Android device to another using only the sender's display and the receiver's camera. The sender displays a looping sequence of animated QR frames. The receiver scans the frames and reconstructs the file. The transfer must not require Wi-Fi, Bluetooth, mobile data, cable, pairing, or a server once the app is installed.

For this first implementation checkpoint, create a clean, buildable vertical slice with:
1. Home screen with Send and Receive routes.
2. Send screen using Android's system document picker to select one file and show filename, MIME type, size, and a placeholder SHA-256 status.
3. Receive screen with camera permission state, a placeholder camera preview state, and explicit error/retry UI.
4. Immutable UI state and ViewModels for Send and Receive flows.
5. Domain package independent from Compose, CameraX, Android Context, and Uri.
6. A versioned FrameEnvelope model with magic prefix, protocol version, flags, 16-byte transfer ID, frame kind, seed, degree, sequence, payload length, payload, and CRC32.
7. Unit-testable serializer/parser interfaces with tests for round-trip, invalid magic, unsupported version, truncated payload, and CRC mismatch.
8. A documented dependency decision point for a JVM/Kotlin-compatible QR encoder/decoder. Do not add NDK or native C/C++ dependencies.
9. A screen-on policy interface for the sender, but keep the implementation lifecycle-safe and release it on stop, cancel, completion, and error.
10. No real network calls and no logging of file contents or payloads.

Use clear package separation: app, domain.model, domain.protocol, domain.integrity, data.file, data.camera, data.qr, presentation.home, presentation.send, and presentation.receive.

Before writing code, show the proposed file tree and the dependency list. Then implement the skeleton and tests. Keep comments concise. Do not claim that camera transfer works until it has been tested on a physical Android device, because the browser emulator may not provide camera capture.

At the end, print build/test commands and list any unresolved decisions, especially the QR library choice and Android SDK version.
```

## 19. Prompt lanjutan per issue

Prompt lanjutan harus diberikan satu per satu setelah checkpoint build hijau.

| Issue | Prompt lanjutan |
|---:|---|
| #14 | “Implement the binary FrameEnvelope serializer/parser and golden vectors from the TDD. Keep the domain layer Android-free and add rejection tests.” |
| #15 | Selesai pada checkpoint QR: ZXing Core 3.5.4, adapter encode/decode, binary fixture round-trip, ADR, dan dependency license note. Physical-device benchmark tetap terbuka. |
| #16 | “Implement sender file picking, streaming SHA-256, metadata frame, sequential chunk adapter, looping QR renderer, pause/cancel, and lifecycle-safe screen-on.” |
| #17 | Selesai pada checkpoint receiver: CameraX Preview/ImageAnalysis `1.4.1`, KEEP_ONLY_LATEST, RGBA analyzer, ImageProxy cleanup, permission states, QR decoder adapter, dan invalid-frame rejection. Physical-device QA tetap terbuka. |
| #18 | Selesai pada checkpoint reconstruction: metadata decoder, bounded sequential temp reconstruction, out-of-order block writes, SHA-256 verification, Unicode filename sanitization, ACTION_CREATE_DOCUMENT save flow, dan cleanup setelah save/failure. Fountain repair frames dan physical-device E2E masih terbuka. |
| #19 | Selesai pada checkpoint fountain: systematic + repair frames, deterministic seed/index selection, incremental GF(2) pivot decoder, dropped/duplicate/out-of-order tests, bounded sender store, dan ADR [`docs/ADR-FOUNTAIN-CODEC.md`](./ADR-FOUNTAIN-CODEC.md). Physical-device recovery-rate benchmark tetap terbuka. |
| #20 | “Refactor sender and receiver into an explicit immutable TransferState reducer with cancellation, lifecycle, rotation, timeout, and cleanup tests.” |
| #21 | “Add local diagnostics without payload logging: timing, unique/duplicate/rejected frames, goodput, source/recovered blocks, and a device benchmark export.” |
| #22 | “Add privacy notice, screen-reader semantics, dark/landscape layout, brightness/distance guidance, screen-on cleanup, and accessible error states.” |
| #23 | “Prepare a physical-device QA checklist and release candidate. Test with Wi-Fi, Bluetooth, and mobile data disabled after installation.” |
| #24 | “Write the final Android README, AI Studio ZIP handoff guide, Android Studio import steps, device test guide, troubleshooting, known limitations, and privacy statement.” |

## 20. Definition of technical completion

TDD dianggap terimplementasi ketika #13 sampai #24 telah melewati acceptance criteria masing-masing, protocol golden vectors lulus, unit/instrumentation test lulus, dan transfer end-to-end berhasil pada minimal dua perangkat Android nyata. Final validation harus membuktikan bahwa file hasil memiliki byte dan SHA-256 yang sama dengan sumber, frame hilang tidak langsung merusak transfer, serta tidak ada request jaringan selama sesi.

## 21. Referensi

[1]: https://ai.google.dev/gemini-api/docs/aistudio-android "Google AI Studio — Build Android Apps"
[2]: https://developer.android.com/media/camera/camerax/analyze "Android Developers — CameraX Image Analysis"
[3]: https://developer.android.com/training/data-storage/shared/documents-files "Android Developers — Access documents and other files from shared storage"
[4]: https://developer.android.com/develop/ui/compose "Android Developers — Jetpack Compose"
[5]: https://github.com/bashalarmistalt/decimen-optical-transfer/ "Decimen Optical Transfer — fountain-coded QR file transfer"
[6]: https://developer.android.com/media/camera/camerax/analyze "Android Developers — CameraX ImageAnalysis"
[7]: https://developer.android.com/media/camera/camerax/preview "Android Developers — CameraX PreviewView"
[8]: https://developer.android.com/training/permissions/requesting "Android Developers — Request runtime permissions"
