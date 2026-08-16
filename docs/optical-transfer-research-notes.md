# Research Notes — Google AI Studio Android

Tanggal: 2026-08-16

Sumber utama: https://ai.google.dev/gemini-api/docs/aistudio-android

Temuan:
- Google AI Studio Build mode saat ini dapat menghasilkan aplikasi Android native dari prompt natural language.
- Project yang dihasilkan berupa Kotlin + Jetpack Compose, Gradle Kotlin DSL, single-activity, single-module, dan client-side only.
- AI Studio menyediakan browser-based Android emulator, tetapi emulator tidak mendukung camera/photo capture; pengujian kamera harus dilakukan pada perangkat fisik.
- APK dapat dipasang ke perangkat fisik melalui Chrome/Edge + WebUSB, Android Developer Options, USB debugging, dan kabel USB.
- Project dapat diunduh sebagai ZIP untuk dilanjutkan di Android Studio.
- Batasan yang relevan: tidak mendukung Java/XML layouts, NDK/native C/C++, Wear OS/Android TV, server-side runtime, Firebase/Workspace APIs/secrets/multiplayer untuk Android app, dan export GitHub belum tersedia untuk Android project.
- Kesimpulan: konsep optical transfer cocok untuk AI Studio karena client-side/native dan tidak membutuhkan backend; namun camera/QR decoder, performance, dan transfer protocol harus diuji di perangkat nyata, bukan hanya emulator.


Sumber tambahan: https://developer.android.com/media/camera/camerax/analyze

Temuan CameraX:
- ImageAnalysis menyediakan frame yang dapat diakses CPU melalui ImageAnalysis.Analyzer untuk computer vision.
- Analyzer harus memproses frame secepat mungkin dan selalu memanggil ImageProxy.close(), bukan Media.Image.close().
- CameraX menyediakan strategi backpressure seperti STRATEGY_KEEP_ONLY_LATEST untuk membuang frame lama ketika analyzer lebih lambat dari input kamera.
- Implikasi desain: QR analyzer harus non-blocking, memakai executor/coroutine terpisah, tidak menahan ImageProxy, dan memakai latest-frame strategy agar preview tidak tersendat.


Sumber tambahan: https://developer.android.com/training/data-storage/shared/documents-files

Temuan Storage Access Framework:
- System picker memberikan kontrol pengguna atas dokumen dan tidak membutuhkan permission storage luas untuk alur yang dipilih pengguna.
- ACTION_OPEN_DOCUMENT cocok untuk memilih file sumber, sedangkan ACTION_CREATE_DOCUMENT cocok untuk menyimpan hasil dengan nama dan MIME type.
- Aplikasi memperoleh akses melalui URI yang dipilih pengguna; implementasi harus membaca/menulis melalui ContentResolver dan tidak mengasumsikan path filesystem biasa.
- Implikasi desain: sender memakai ActivityResultContracts.OpenDocument atau ACTION_OPEN_DOCUMENT; receiver memakai CreateDocument atau ACTION_CREATE_DOCUMENT; temp reconstruction memakai app-specific storage dan dibersihkan setelah sesi selesai.

## QR codec spike — 2026-08-16

Kandidat utama adalah `com.google.zxing:core:3.5.4`. Sumber resmi ZXing menyatakan library ini adalah pemroses barcode 1D/2D berbasis Java, mendukung QR Code, dan berlisensi Apache-2.0. Maven Central mencantumkan artifact `core:3.5.4` tanpa dependency runtime tambahan selain Java platform; API ini menyediakan jalur encode dan decode yang sesuai untuk adapter Android-free.

Kandidat pembanding adalah `io.github.g0dkar:qrcode-kotlin:4.5.0`. Repositori resminya menyatakan library ini pure Kotlin, ringan, tanpa dependency tambahan, mendukung Android/JVM/KMP, dan berlisensi MIT. Namun fokus API publiknya adalah generator QR; decoder QR tidak tersedia sebagai pasangan yang setara untuk receiver.

Keputusan sementara: gunakan ZXing Core 3.5.4 sebagai codec dua arah untuk MVP karena satu artifact dapat menjadi encoder dan decoder, tidak membutuhkan NDK/native C++, dan berlisensi Apache-2.0. Risiko keputusan ini adalah upstream ZXing berada dalam maintenance mode, sehingga integrasi harus dibatasi melalui interface `QrEncoder`/`QrDecoder` agar dapat diganti jika physical-device benchmark tidak memenuhi target.

Referensi: https://github.com/zxing/zxing, https://central.sonatype.com/artifact/com.google.zxing/core, https://github.com/g0dkar/qrcode-kotlin, https://central.sonatype.com/artifact/io.github.g0dkar/qrcode-kotlin-jvm
