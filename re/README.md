# Reverse Engineering Analysis: MASA vs NU Online (Qibla & Compass Implementation)

Hasil reverse engineering aplikasi Android yang diambil dari perangkat target (`adb pull`) untuk membandingkan algoritma perhitungan kiblat, pemrosesan sensor kompas/gyroscope, dan akuisisi GPS.

---

## 1. Ringkasan Target APK

| Properti | MASA (`com.labmu.masa`) | NU Online (`id.or.nu.app`) |
|---|---|---|
| **Version** | `2.10.1` (`versionCode: 173`) | `2.29.0` (`versionCode: 4377`) |
| **Framework** | Flutter (Dart `3.9.2` AOT ARM64) | Flutter (Dart `3.13.7` AOT ARM64) |
| **Compass Plugin** | `flutter_compass` (`com.hemanthraj.fluttercompass`) | `flutter_device_compass` (`R5/a.java`, `R5/b.java`, `H2/D.java`) |
| **Qibla Math Library** | Custom Dart (`QiblaRepositoryImpl.calculateQiblaDirection`) | `package:adhan` (`QiblaUtil.calculateQiblaDirection`) |
| **Location Plugin** | `geolocator` (`FusedLocationProviderClient` / `LocationManager`) | `geolocator` (`FusedLocationProviderClient` / `LocationManager`) |
| **AR / Web Mode** | Tidak ada (kompas 2D) | Ada (`GoogleQiblaFinderView` -> `https://qiblafinder.withgoogle.com`) |

Direktori artefak bukti RE yang disertakan pada repositori (file APK, `extracted/`, `NOTICES.txt`, dan kelas `jadx` non-kompas dikecualikan lewat `.gitignore`):
- [masa/disasm_qibla.txt](masa/disasm_qibla.txt), [masa/jadx/sources/p134n8/a.java](masa/jadx/sources/p134n8/a.java), [masa/jadx/sources/p134n8/b.java](masa/jadx/sources/p134n8/b.java)
- [nu_online/disasm_qibla.txt](nu_online/disasm_qibla.txt), [nu_online/jadx/sources/R5/a.java](nu_online/jadx/sources/R5/a.java), [nu_online/jadx/sources/R5/b.java](nu_online/jadx/sources/R5/b.java)

---

## 2. Perbandingan Algoritma Perhitungan Sudut Kiblat (Dart AOT `libapp.so`)

### A. MASA (`com.labmu.masa`)
Dari disassembly ARM64 pada `_kDartIsolateSnapshotInstructions` offset `0xb3f9c8..0xb3fc58` ([masa/disasm_qibla.txt](masa/disasm_qibla.txt)) dan konstanta ObjectPool di `0x4c48c3..0x4c48eb`:

- **Koordinat Ka'bah (hardcoded, pre-folded ke radian)**:
  - $\phi_K = 21.4225^\circ\text{ N}$ (`0.37389315900848524 rad` @ `0x4c48d7`)
  - $\lambda_K = 39.8262^\circ\text{ E}$ (`0.69509831855776572 rad` @ `0x4c48c3`)
- **Model Bumi**: **Bola sempurna (Spherical Great-Circle Initial Bearing)**, bukan ellipsoid WGS84 (meski app mengimpor `latlong2` untuk fitur masjid, fungsi `calculateQiblaDirection` mengimplementasikan trigonometri bola langsung di Dart):
  ```dart
  final lat1 = userLat * (pi / 180.0);
  final lon1 = userLon * (pi / 180.0);
  const lat2 = 21.4225 * (pi / 180.0);
  const lon2 = 39.8262 * (pi / 180.0);
  final dLon = lon2 - lon1;

  final y = sin(dLon) * cos(lat2);
  final x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon);
  final qiblaDeg = (atan2(y, x) * (180.0 / pi) + 360.0) % 360.0;
  ```

### B. NU Online (`id.or.nu.app`)
Dari disassembly ARM64 pada `_kDartSnapshotText` offset `0xdb5e80..0xdb60e4` dan `0x88a94c..0x88a9bc` ([nu_online/disasm_qibla.txt](nu_online/disasm_qibla.txt)) serta konstanta ObjectPool di `0x4c1a2a..0x4c1a35`:

- **Koordinat Ka'bah (`Coordinates makkah` di `package:adhan`)**:
  - $\phi_K = 21.4225241^\circ\text{ N}$ (`0x4c1a2a`)
  - $\lambda_K = 39.8261818^\circ\text{ E}$ (`0x4c1a35`)
- **Model Bumi**: **Bola sempurna (Spherical Law of Cotangents / Tangent form dari Great-Circle)**:
  Secara matematis identik dengan pembagian pembilang dan penyebut rumus MASA dengan $\cos(\phi_K)$:
  ```dart
  final dLon = radians(makkah.longitude) - radians(user.longitude);
  final y = sin(dLon);
  final x = cos(radians(user.latitude)) * tan(radians(makkah.latitude))
          - sin(radians(user.latitude)) * cos(dLon);
  final qiblaDeg = unwindAngle(atan2(y, x) * (180.0 / pi)); // x - floor(x / 360.0) * 360.0
  ```

### C. Selisih Numerik Teoretis (Contoh Titik Uji Yogyakarta `-7.8000°, 110.3800°`)
- **MASA (Spherical `21.4225, 39.8262`)**: `294.709566°`
- **NU Online (Spherical `adhan`)**: `294.709585°` (selisih vs MASA hanya `-0.000020°`)
- **Referensi Geodesik Ellipsoid WGS84 (Vincenty)**: `294.582318°` (rumus bola lebih besar `+0.127°` dari WGS84)

---

## 3. Perbandingan Pemrosesan Sensor Kompas & Gyroscope (Native Android `jadx`)

Sumber sebenarnya mengapa bacaan kedua aplikasi di lapangan bisa berbeda atau bergeser bukan di rumus sudut kiblat, melainkan di **pipeline sensor Android**:

| Aspek Sensor | MASA (`p134n8/a.java`, `p134n8/b.java`) | NU Online (`R5/a.java`, `R5/b.java`, `H2/D.java`) |
|---|---|---|
| **Sensor Utama** | `Sensor.TYPE_ROTATION_VECTOR` (`11`, 9-DoF fusi OS: Accel + Mag + **Gyro**) | `Sensor.TYPE_ROTATION_VECTOR` (`11`), mengambil 3 komponen vektor `[x, y, z]` |
| **Sensor Fallback** | `TYPE_ACCELEROMETER` (`1`) + `TYPE_MAGNETIC_FIELD` (`2`) | `TYPE_ACCELEROMETER` (`1`) + `TYPE_MAGNETIC_FIELD` (`2`) + fallback cross-product manual |
| **Low-Pass Filter (Fallback)** | `alpha = 0.45f` (lebih responsif, lebih berisik jika fallback) | `alpha = 0.15f` (lebih halus, lebih lambat merespons jika fallback) |
| **Throttle Interval** | `32 ms` (`~31 Hz`), `registerListener(..., 30000)` | `33 ms` (`updateIntervalMs = 33`), `registerListener(..., 33000)` |
| **Deadband (`headingFilter`)** | Tidak ada di native | `0.1°` (perubahan `< 0.1°` dibuang/diabaikan) |
| **Remapping Tilt** | Otomatis remap axis jika `|pitch| > 45°` (`π/4`) atau `|roll| > 90°` (`π/2`) | Tetap memakai remap layar datar untuk `forUi`, dan `remapCoordinateSystem(R, 1, 3)` terpisah untuk `forCamera` |
| **Koreksi Deklinasi Magnetik (`GeomagneticField`)** | **TIDAK ADA** (output langsung `Math.toDegrees(fArr3[0])` = **Utara Magnetik**) | **TIDAK ADA** (output langsung **Utara Magnetik**) |
| **Range Output Heading** | `[-180°, +180°]` | `[0°, 360°)` |

### Implikasi Penting untuk Pengukuran & Paper
1. **Magnetic North vs True North**: Kedua aplikasi sama-sama **tidak menggunakan `android.hardware.GeomagneticField`** (World Magnetic Model) untuk mengoreksi deklinasi magnetik lokal, sehingga azimuth kompas masih mengacu ke Utara Magnetik bukan Utara Sejati (True North).
2. **Magnetic Anomaly di Masjid**: `TYPE_ROTATION_VECTOR` tetap mengandalkan magnetometer untuk yaw absolut. Di dalam bangunan masjid (baja tulangan beton, kerangka kubah/atap baja, speaker, AC), medan magnet bumi terdistorsi dan menyebabkan error beberapa derajat—tetapi **MASA dan NU Online sama sekali tidak menampilkan kekuatan medan magnet ($\mu\text{T}$), gyro angular velocity ($\text{rad/s}$), maupun kemiringan (pitch/roll)**.
3. **Alasan Aplikasi Custom Diperlukan**: Dengan aplikasi kita sendiri, kita bisa mencatat seluruh data mentah sensor (`Accelerometer`, `Gyroscope`, `Magnetometer` $\mu\text{T}$, `Rotation Vector`, `Game Rotation Vector`, `GeomagneticField` declination, `GPS` lat/lon/accuracy) dan menyimpannya ke CSV/JSON untuk dianalisis secara kuantitatif di paper.
