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
