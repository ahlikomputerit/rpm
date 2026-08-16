# ADR — QR Codec untuk Lumen Transfer

**Status:** Accepted for MVP, pending physical-device benchmark  
**Tanggal:** 2026-08-16  
**Terkait:** Issue #15, TDD sections 5, 8, 13, dan 14

## Context

Lumen Transfer membutuhkan encoder dan decoder QR untuk alur dua arah: pengirim mengubah `ByteArray` frame protocol menjadi QR, sedangkan penerima mengubah hasil kamera kembali menjadi `ByteArray`. Dependency harus dapat di-resolve melalui Gradle, kompatibel dengan Android/Kotlin/JVM, tidak membawa NDK/native C++, dan dapat dibungkus di balik interface agar domain protocol tetap Android-free.

## Candidates

| Kandidat | Kekuatan | Risiko |
|---|---|---|
| `com.google.zxing:core:3.5.4` | API Java matang untuk encode dan decode QR; artifact Maven Central; Apache-2.0; tidak membutuhkan NDK untuk core path. | Upstream berada dalam maintenance mode; throughput dan robustness pada kamera ponsel tetap harus diuji. |
| `io.github.g0dkar:qrcode-kotlin:4.5.0` | Pure Kotlin, ringan, tanpa dependency tambahan, mendukung Android/JVM, MIT. | Fokus API publik adalah generator; belum menjadi pasangan decoder yang lengkap untuk receiver. |
| `zxing-cpp` atau wrapper native lain | Potensi performa decode tinggi. | Native/NDK bertentangan dengan batasan MVP Google AI Studio dan menambah kompleksitas ABI, packaging, serta device QA. |

## Decision

Gunakan **`com.google.zxing:core:3.5.4`** untuk MVP. Implementasi production tidak boleh mengimpor ZXing langsung ke domain. Semua akses harus melalui `QrEncoder` dan `QrDecoder` pada package `data.qr`.

Encoder akan menghasilkan matrix modul QR dari byte frame. Decoder akan menerima luminance/binary image yang sudah diekstrak oleh CameraX analyzer pada tahap berikutnya. Adapter sementara boleh memakai Base64 URL-safe untuk mengubah frame biner menjadi string, tetapi Base64 bukan bagian dari kontrak domain dan overhead-nya harus diukur sebelum payload budget ditetapkan.

## Consequences

Keputusan ini memberikan satu dependency dua arah yang ringan dan menghindari NDK. Konsekuensinya, kode harus mengisolasi API ZXing, tidak boleh menganggap maintenance mode sebagai jaminan performa, dan harus menyediakan fixture encode/decode serta benchmark pada perangkat Android nyata. Jika library gagal pada payload biner, QR version, atau frame rate yang dibutuhkan, interface tetap dapat dipindahkan ke library lain tanpa mengubah serializer, scheduler, atau ViewModel.

## Validation plan

Spike dianggap lulus apabila Gradle dapat me-resolve dependency, encoder menghasilkan matrix yang memiliki quiet zone dan finder pattern valid, decoder mengembalikan payload yang sama pada fixture, payload protocol `FrameSerializer.serialize()` dapat melewati round-trip, dan tidak ada NDK/native library yang masuk ke APK. Pengujian kamera dan throughput belum dapat dinyatakan lulus tanpa perangkat Android nyata.

## References

[1]: https://github.com/zxing/zxing "ZXing official repository and license"
[2]: https://central.sonatype.com/artifact/com.google.zxing/core "Maven Central — com.google.zxing:core:3.5.4"
[3]: https://github.com/g0dkar/qrcode-kotlin "QRCode-Kotlin official repository and license"
[4]: https://central.sonatype.com/artifact/io.github.g0dkar/qrcode-kotlin-jvm "Maven Central — qrcode-kotlin-jvm:4.5.0"
