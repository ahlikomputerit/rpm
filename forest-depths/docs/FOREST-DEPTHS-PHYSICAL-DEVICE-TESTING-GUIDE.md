# Forest Depths — Panduan Pengujian Perangkat Fisik Android dan iOS

**Tujuan:** memvalidasi performa WebGL, kestabilan frame, fallback, interaksi, dan perilaku thermal Forest Depths pada hardware nyata.  
**Status:** QA guide  
**Versi:** 1.0  
**Pemilik:** Tim QA / Engineering  
**Target minimal:** satu perangkat Android dan satu perangkat iPhone fisik, ditambah satu perangkat low-end atau older-generation bila tersedia.

## 1. Mengapa pengujian fisik diperlukan

Pengujian viewport mobile di desktop hanya mengubah ukuran layar dan input model. Ia tidak merepresentasikan GPU, driver WebGL, bandwidth memori, thermal throttling, battery state, atau compositor milik perangkat nyata. Pada sandbox sebelumnya, renderer yang terdeteksi adalah SwiftShader/software rendering dan frame-rate sangat rendah; hasil tersebut berguna sebagai stress signal, tetapi bukan bukti performa Android/iOS.

Pengujian fisik harus mengukur **frame time distribution**, bukan hanya angka FPS rata-rata. Secara praktis, 60 FPS berarti sekitar 16,7 ms per frame dan 30 FPS sekitar 33,3 ms per frame. Frame time P95, jumlah long frame, konsistensi selama scroll, dan perubahan setelah perangkat panas lebih penting daripada satu burst FPS singkat [3].

## 2. Test matrix yang direkomendasikan

Gunakan device matrix berikut sebagai baseline. Catat model, OS, browser, GPU/SoC, DPR, orientasi, dan status battery/thermal untuk setiap run.

| Tier | Android | iOS | Tujuan |
|---|---|---|---|
| High | Pixel 8/9 atau Galaxy S23/S24, Chrome terbaru | iPhone 14/15/16, Safari terbaru | Memastikan pengalaman premium pada hardware modern |
| Mid | Pixel 6a/7a atau Galaxy A54, Chrome terbaru | iPhone 11/12/13, Safari terbaru | Target utama kualitas mobile yang realistis |
| Low/legacy | Android device dengan GPU Adreno/Mali generasi lebih lama | iPhone SE 2 atau device lama yang masih didukung | Menguji graceful degradation, bukan target visual maksimum |

Jika hanya dua perangkat tersedia, pilih satu Android mid/high dan satu iPhone mid/high. Tambahkan perangkat low-end sebelum rilis publik bila traffic analytics memperlihatkan mayoritas pengguna memakai hardware lama.

### Baseline yang harus sama

Setiap run harus menggunakan build/commit yang sama, URL yang sama, portrait orientation, browser release yang dicatat, cache state yang dicatat, dan kondisi network yang sama. Jalankan satu cold-load setelah browser benar-benar ditutup, lalu satu warm-load setelah resource cache terbentuk. Jangan mencampur hasil cold-load dan warm-load dalam satu angka.

Sebelum mengukur, isi kondisi berikut:

| Field | Nilai yang dicatat |
|---|---|
| Device model / storage | Contoh: Pixel 7a / 128 GB |
| OS version | Android atau iOS exact version |
| Browser version | Chrome atau Safari exact version |
| SoC/GPU | Jika dapat diidentifikasi |
| Device pixel ratio | Nilai `window.devicePixelRatio` |
| Viewport | CSS width × height dan orientation |
| Battery | Persentase dan charging/not charging |
| Thermal state | Cool / warm / hot; jangan mulai sambil charging |
| Network | Wi-Fi, 4G, 5G, throttled, atau offline cache |
| Build commit | Git SHA yang diuji |
| Motion preference | Normal atau reduced motion |
| Audio state | Muted atau enabled after user gesture |

## 3. Menyiapkan URL yang dapat diakses perangkat

Perangkat fisik harus membuka URL yang benar-benar dapat dijangkau. Gunakan deployment staging/preview HTTPS yang menunjuk ke commit yang hendak diuji. Hindari mengukur melalui screen-share atau proxy yang menyuntikkan konten tambahan.

Jika perlu menguji server lokal, gunakan jaringan lokal yang sama dan bind Vite ke network interface, lalu buka alamat LAN dari perangkat. Pastikan firewall mengizinkan port preview. Untuk hasil yang dapat dibandingkan antar-device, deployment HTTPS staging lebih baik karena menghindari perbedaan routing dan mixed-content behavior.

Sebelum profiling, buka URL satu kali secara normal dan pastikan seluruh asset `/manus-storage/` dapat diakses. Catat request yang gagal sebelum mulai frame measurement.

## 4. Setup Android dengan Chrome DevTools

Chrome mendukung remote inspection perangkat Android melalui desktop Chrome. Prosedur resminya menggunakan Developer Options, USB Debugging, `chrome://inspect`, dan authorization prompt pada perangkat [1].

### 4.1 Device preparation

1. Buka **Settings → About phone** dan aktifkan Developer Options melalui nomor build jika belum aktif.
2. Buka **Developer Options** dan aktifkan **USB debugging**.
3. Pastikan layar perangkat tidak terkunci saat pertama kali disambungkan.
4. Gunakan kabel data langsung, bukan USB hub, untuk baseline pertama.
5. Hubungkan perangkat ke komputer dan setujui prompt **Allow USB debugging**.
6. Buka desktop Chrome dan navigasikan ke `chrome://inspect`.
7. Aktifkan **Discover USB devices**.
8. Pastikan model perangkat tampil dan remote Chrome tab dapat dipilih.

Chrome DevTools menyediakan screencast, tetapi screencast dapat menurunkan frame rate. **Matikan screencast selama pengukuran performa** dan gunakan screencast hanya untuk menemukan posisi visual atau merekam bukti interaksi [1].

### 4.2 Android test flow

Buka halaman Forest Depths di Chrome Android melalui remote tab. Pada DevTools, lakukan pengecekan berikut sebelum scroll:

| Check | Tindakan |
|---|---|
| Console | Pastikan tidak ada exception, WebGL error, atau failed texture request |
| Network | Pastikan WebP/PNG storage requests sukses dan tidak ada repeated retry |
| Elements | Verifikasi canvas, chapter buttons, hotspot button, dan audio control |
| Console probe | Jalankan probe frame-time yang disediakan di bagian 8 |
| Device state | Catat DPR, viewport, battery, dan thermal state |

Jalankan test route **01 → 05 → 09**. Pada setiap route, tunggu 3 detik tanpa scroll untuk baseline, lalu lakukan scroll kontinu sekitar 5–8 detik. Ulangi scroll naik dan turun agar transisi chapter diuji dua arah.

### 4.3 Optional ADB/CDP route

Jika `chrome://inspect` tidak cukup, gunakan ADB. Verifikasi device dengan:

```bash
adb devices -l
adb forward tcp:9222 localabstract:chrome_devtools_remote
```

Kemudian cek `http://localhost:9222/json` untuk memastikan remote page target tersedia. Jalur ini berguna untuk automation atau mengambil metadata CDP secara programmatic [1].

## 5. Setup iOS dengan Safari Web Inspector

Web Inspector iOS/iPadOS diaktifkan melalui **Settings → Apps → Safari → Advanced → Web Inspector**. Setelah itu perangkat disambungkan ke Mac, trust prompt disetujui, dan halaman muncul pada Safari **Develop** menu [2].

### 5.1 Device preparation

1. Buka **Settings → Apps → Safari → Advanced**.
2. Aktifkan **Web Inspector**.
3. Hubungkan iPhone ke Mac dengan kabel.
4. Setujui **Trust This Computer** pada iPhone jika prompt muncul.
5. Buka Safari di Mac.
6. Buka menu **Develop** dan pastikan perangkat iPhone muncul.
7. Buka halaman Forest Depths di Safari iPhone.
8. Pilih tab halaman tersebut dari **Develop → [device]** untuk membuka Web Inspector.

Wired connection diprioritaskan untuk baseline. Setelah koneksi kabel pertama berhasil, inspection over network dapat diaktifkan untuk iterasi berikutnya, tetapi wired run tetap dipakai sebagai sumber hasil resmi bila terdapat perbedaan koneksi [2].

### 5.2 iOS profiling flow

Gunakan tab **Console**, **Network**, **Timelines**, **Graphics**, dan **Layers** sesuai kebutuhan. Timelines dapat merekam network, layout/rendering, JavaScript/events, CPU, memory, dan frame-oriented activity [4]. Frames view menampilkan waktu tiap rendering frame dengan garis referensi sekitar 30 FPS dan 60 FPS [3].

Mulai recording sebelum memasuki chapter 01, hentikan setelah chapter 09, lalu ekspor recording. Ulangi dengan recording yang hanya mencakup chapter 05 untuk isolasi middle-state.

## 6. Prosedur pengujian fungsional E2E

Jalankan langkah berikut pada kedua platform. Satu tester menjalankan flow; satu observer mencatat timestamp, chapter, visual anomaly, dan console/network state.

| Step | Expected result |
|---:|---|
| 1 | Cold-load menampilkan hero dan image fallback tanpa blank page |
| 2 | Header, menu, depth rail, audio button, dan CTA terlihat serta dapat diakses |
| 3 | Menu chapter terbuka dan menampilkan 9 chapter |
| 4 | Pilih chapter 02; scroll/jump mencapai Fern Passage dan active stage sinkron |
| 5 | Tap/focus `FERN UNDERSTOREY`; observation card terbuka |
| 6 | Pilih chapter 05; camera/environment berubah dan story copy tetap terbaca |
| 7 | Pilih chapter 09; Heartwood hotspot membuka observation 09 |
| 8 | Tap audio; state berubah ON/OFF dan tidak ada exception |
| 9 | Scroll berhenti sekitar 3 detik setelah melakukan progress; hidden observation dapat muncul |
| 10 | Reload pada deep chapter; page kembali stabil dan resource tidak berlipat tanpa batas |
| 11 | Tekan browser back/forward jika route harness digunakan; tidak ada dead end |
| 12 | Putar device orientation bila produk mendukung; tidak ada horizontal overflow atau blank canvas |

Untuk test keyboard, gunakan external keyboard pada Android/iPad bila tersedia. Di mobile touch, pastikan hotspot memiliki ukuran tap yang cukup dan tidak bergantung pada hover.

## 7. Prosedur frame-time dan WebGL profiling

### 7.1 Prinsip pengukuran

Jangan mengukur ketika DevTools screencast aktif, device sedang charging, browser baru melakukan update, atau ada aplikasi berat lain di foreground. Pastikan brightness dan network stabil. Catat apakah audio aktif karena Web Audio dapat menambah workload berbeda dari muted mode.

Ukur tiga segmen:

| Segment | Durasi | State |
|---|---:|---|
| Idle baseline | 5 s | Chapter 01, tanpa scroll |
| Continuous journey | 8–10 s | Scroll 01 → 05 → 09 |
| Deep state | 5 s | Chapter 09, tanpa scroll |

Ulangi setiap segmen tiga kali. Buang hanya run yang jelas invalid, misalnya lock-screen, tab background, permission prompt, atau thermal warning; jangan membuang run hanya karena FPS rendah.

### 7.2 Metrik minimum

Catat `frames`, elapsed duration, mean frame time, P50, P95, P99 frame time, frames over 33.3 ms, frames over 50 ms, approximate average FPS, long-frame ratio, renderer string, DPR, canvas pixel dimensions, JS errors, failed network requests, dan device temperature/thermal state bila tersedia.

Interpretasi praktis:

| Frame time | Approximate ceiling | Meaning |
|---:|---:|---|
| ≤16.7 ms | 60 FPS | Smooth 60-oriented frame budget |
| ≤33.3 ms | 30 FPS | Acceptable cinematic mobile floor |
| >33.3 ms | <30 FPS | Visible hitch risk during scroll |
| >50 ms | <20 FPS | Long frame; investigate |
| >100 ms | <10 FPS | Severe hitch or blocking workload |

### 7.3 Browser console probe

Pada Chrome Android, jalankan probe sederhana berikut di Console. Jalankan selama 5–10 detik pada setiap segment dan simpan output bersama device metadata:

```js
(async () => {
  const canvas = document.querySelector("canvas.forest-three-canvas");
  const gl = canvas?.getContext("webgl2") || canvas?.getContext("webgl");
  const debug = gl?.getExtension("WEBGL_debug_renderer_info");
  const start = performance.now();
  const deltas = [];
  let last = start;
  let frames = 0;

  await new Promise((resolve) => {
    const tick = (now) => {
      if (frames > 0) deltas.push(now - last);
      last = now;
      frames += 1;
      if (now - start < 5000) requestAnimationFrame(tick);
      else resolve();
    };
    requestAnimationFrame(tick);
  });

  const sorted = [...deltas].sort((a, b) => a - b);
  const percentile = (p) => sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * p))] || 0;
  const mean = deltas.reduce((sum, value) => sum + value, 0) / Math.max(1, deltas.length);

  console.table({
    viewport: `${innerWidth}x${innerHeight}`,
    dpr: devicePixelRatio,
    canvas: canvas ? `${canvas.width}x${canvas.height}` : "none",
    renderer: gl && debug ? gl.getParameter(debug.UNMASKED_RENDERER_WEBGL) : gl?.getParameter(gl.RENDERER) || "none",
    frames,
    avgFps: Number((1000 / mean).toFixed(2)),
    meanFrameMs: Number(mean.toFixed(2)),
    p50FrameMs: Number(percentile(0.5).toFixed(2)),
    p95FrameMs: Number(percentile(0.95).toFixed(2)),
    p99FrameMs: Number(percentile(0.99).toFixed(2)),
    over33ms: deltas.filter((value) => value > 33.3).length,
    over50ms: deltas.filter((value) => value > 50).length,
  });
})();
```

The probe is supplemental. Use Chrome Performance recording or Safari Web Inspector Timelines/Frames as the primary evidence because they show rendering activity and frame-level breakdown rather than only callback timing [3] [4].

## 8. Chapter stress profiles

### Chapter 01 — Forest Edge

This is the light-state baseline. Confirm that first paint is immediate, lazy Three.js does not block text, scene plates are visible, and particles do not create a noticeable startup spike.

### Chapter 05 — Cathedral Grove

This is the composition stress state. Confirm camera path, trunk/canopy geometry, textured plates, PNG cedar hotspot, weather particles, fog curtain, and hidden observation coexist without sustained long frames. Record whether thermal or memory behavior changes after the first four chapters.

### Chapter 09 — Heartwood

This is the deep-state stress test. Confirm higher fog, deeper camera position, Heartwood PNG hotspot, observation card, firefly/hidden detail, audio state, and dark compositing. Test both muted and enabled audio because the acoustic layer changes runtime behavior.

## 9. Thermal and battery protocol

Run the first pass on a cool, unplugged device. Record battery percentage before and after the three segments. Wait 3–5 minutes between repeated full journeys and note whether the device becomes warm or the OS reports thermal pressure.

Run a second pass after 15–20 minutes of repeated 01 → 05 → 09 journeys. Compare P95 frame time and long-frame ratio against the cool baseline. A large degradation after warming indicates thermal sensitivity even if the first run passed.

Do not use charging as the official performance baseline. Charging changes thermal behavior and can mask the conditions users experience during ordinary browsing.

## 10. Acceptance criteria

The following criteria are recommended for a first mobile release. They should be agreed with the product owner before converting them into a release gate.

| Criterion | Pass recommendation |
|---|---|
| Functional E2E | All mandatory steps pass on Android and iOS |
| Console | No uncaught exceptions, WebGL context loss loop, or repeated asset failure |
| Cold first paint | Hero/copy visible before Three.js initialization; no blank state |
| Mid-tier continuous journey | P95 frame time ≤33.3 ms or documented exception; no sustained severe hitch |
| Long frames | No repeated sequence of >50 ms frames during ordinary scroll on target devices |
| Deep state | Chapter 09 remains readable and interactive for 5 s idle and 8 s scroll transition |
| Reduced motion | Three.js animation is not required; narrative and controls remain complete |
| Fallback | WebGL unavailable or context creation fails without root crash |
| Thermal stability | After warm run, no catastrophic frame-time regression or browser termination |
| Memory/resource behavior | No repeated texture loading loop, tab reload, context-loss loop, or runaway memory symptom |

For low-end devices, a lower visual-quality tier may be accepted if the page disables optional particles or reduces pixel ratio while preserving narrative and controls. Never mark a run pass solely because average FPS is high if P95/P99 frame time shows repeated hitching.

## 11. Troubleshooting

### Android device is not visible

Unlock both screens, reconnect directly without a hub, verify USB debugging, accept the device authorization prompt, and update OEM drivers on Windows if necessary. Chrome’s official guide also recommends checking cable/data mode and revoking USB debugging authorizations when the prompt becomes stale [1].

### iPhone is not visible in Safari Develop

Verify Web Inspector under Safari Advanced settings, reconnect the cable, accept Trust This Computer, unlock the iPhone, and restart Safari if the Develop submenu remains empty. Use a cable for the first connection before enabling network inspection [2].

### Canvas is missing

Inspect console and WebGL probe results. A missing canvas may be intentional under reduced motion or WebGL failure. Confirm that `matchMedia('(prefers-reduced-motion: reduce)').matches` is false for the normal-motion test. Check whether browser privacy/settings, headless mode, Low Power Mode, or a context-creation error caused the fallback.

### Frame rate drops only while recording

Disable screencast and minimize inspector overhead. Chrome documents that screencasting negatively affects frame rate [1]. Safari Timelines is designed to reduce some debugging overhead during recordings, but compare a normal run with a recorded run and report the method used [3].

### Performance degrades over time

Record temperature and battery state. Compare cool and warm runs. Reduce mobile pixel ratio first, then optional dust/pollen/leaf counts, fog curtains, and hidden idle visuals. Keep the CSS fallback and HTML narrative intact.

### Asset requests fail

Check the Network panel for `/manus-storage/` status codes, response timing, content type, and repeated retries. Verify that the test URL is HTTPS/staging and not blocked by device network, content blocker, or captive portal.

## 12. Test report template

Copy this table for each device and attach the exported DevTools/Safari recording where possible.

| Field | Value |
|---|---|
| Tester / date |  |
| Git commit / URL |  |
| Device model / OS |  |
| Browser version |  |
| SoC/GPU |  |
| DPR / viewport |  |
| Battery / charging |  |
| Thermal state before/after |  |
| Network condition |  |
| Motion preference |  |
| Audio state |  |
| Renderer string |  |
| Canvas dimensions |  |
| Chapter segment | 01 / 05 / 09 |
| Run type | Cold / warm |
| Frames / duration |  |
| Average FPS |  |
| P50 frame time |  |
| P95 frame time |  |
| P99 frame time |  |
| Frames >33.3 ms |  |
| Frames >50 ms |  |
| Console errors |  |
| Network failures |  |
| Thermal/battery notes |  |
| Visual anomalies |  |
| Result | Pass / Fail / Conditional |

## 13. References

[1]: https://developer.chrome.com/docs/devtools/remote-debugging "Chrome DevTools — Remote debug Android devices"
[2]: https://developer.apple.com/documentation/safari-developer-tools/inspecting-ios "Apple Developer — Inspecting iOS and iPadOS"
[3]: https://webkit.org/web-inspector/timelines-tab/ "WebKit — Web Inspector Timelines Tab"
[4]: https://developer.apple.com/documentation/safari-developer-tools/web-inspector "Apple Developer — Web Inspector"
[5]: https://developer.mozilla.org/en-US/docs/Web/API/Window/requestAnimationFrame "MDN — requestAnimationFrame"
