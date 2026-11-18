# QiblaKu — Aplikasi Pengukur Kiblat 50 Hz, Paper PDF, & Bukti Reverse Engineering

Repositori ini berisi naskah akhir **Paper PDF**, kode sumber aplikasi Android **QiblaKu** (`com.ireddragonicy.qiblaku`), serta artefak bukti *reverse engineering* terhadap aplikasi kompas kiblat **MASA** (`com.labmu.masa` v2.10.1) dan **NU Online** (`id.or.nu.app` v2.29.0).

---

## Struktur Repositori

| Direktori / Berkas | Deskripsi |
|---|---|
| [`paper/Paper_Analisis_Arah_Kiblat_UAD.pdf`](paper/Paper_Analisis_Arah_Kiblat_UAD.pdf) | Naskah akhir paper 5 halaman mengenai evaluasi akurasi arah kiblat Masjid Kampus 1–5 UAD berbasis telemetri sensor IMU 50 Hz dan rekayasa balik aplikasi mobile (*source pembuatan paper tidak disertakan, hanya PDF final*). |
| [`qiblaku-app/`](qiblaku-app/) | Aplikasi Android *native* (Kotlin, Jetpack Compose, `minSdk 26`, `targetSdk 36`) untuk perhitungan kiblat geodesik **WGS84 (Vincenty)** dan model bola pembanding, koreksi deklinasi **WMM (`GeomagneticField`)**, kompas fusi **9-DoF** vs 6-DoF/LPF, serta perekam telemetri IMU **CSV 50 Hz**. |
| [`re/`](re/) | Ringkasan analisis & artefak bukti teknis *reverse engineering* MASA vs NU Online: [`re/README.md`](re/README.md), disassembly ARM64 Dart AOT (`disasm_qibla.txt`), dan kelas Java pembacaan sensor kompas Android (`p134n8/a.java`, `p134n8/b.java`, `R5/a.java`, `R5/b.java`). |
| [`LICENSE`](LICENSE) | Lisensi MIT untuk kode sumber proyek, paper PDF, dan dokumentasi analisis. |

---

## Fitur Utama `qiblaku-app`

- **Geodesik WGS84 & Pembanding Bola**: Menghitung azimuth sejati menuju Ka'bah (`21.42249° N, 39.82617° E`) dengan algoritma iteratif Vincenty pada ellipsoid WGS84 sekaligus menampilkan rumus bola pembanding (*Spherical Great-Circle* ala MASA dan *Spherical Cotangent* `adhan` ala NU Online).
- **Koreksi Deklinasi Magnetik (WMM)**: Menggunakan `android.hardware.GeomagneticField` untuk konversi otomatis Utara Magnetik → Utara Sejati dan membaca intensitas medan normal $F_{\mathrm{WMM}}$ ($\mu\text{T}$) untuk mendeteksi anomali magnetik lokal ($\Delta |B|$).
- **Pembandingan Pipeline Sensor 8 Kanal (50 Hz)**: Merekam secara simultan `TYPE_ROTATION_VECTOR` (9-DoF), `TYPE_GEOMAGNETIC_ROTATION_VECTOR` (6-DoF tanpa gyro), `TYPE_GAME_ROTATION_VECTOR`, gyroscope (`rad/s`), magnetometer ($\mu\text{T}$), accelerometer ($\text{m/s}^2$), *pitch*/*roll*, dan emulasi LPF ($\alpha = 0.45$ & $\alpha = 0.15$) ke berkas CSV.
- **Leveling & Lock GPS**: Indikator *bubble spirit level* 2D ($|\text{pitch}|, |\text{roll}| \le 2.5^\circ$) serta kunci koordinat GPS sebelum memasuki ruang dalam masjid.

---

## Bukti Reverse Engineering (`re/`)

File biner APK pihak ketiga, `lib*.so`, `NOTICES.txt`, dan paket `jadx` non-kompas dikecualikan lewat `.gitignore`. File bukti inti yang disertakan:

- [`re/README.md`](re/README.md) — Penjabaran lengkap persamaan kiblat, konstanta koordinat Ka'bah pada ObjectPool Dart AOT, serta parameter sensor native kedua aplikasi.
- [`re/masa/disasm_qibla.txt`](re/masa/disasm_qibla.txt) — Disassembly ARM64 fungsi `QiblaRepositoryImpl.calculateQiblaDirection` (`libapp.so` MASA).
- [`re/masa/jadx/sources/p134n8/a.java`](re/masa/jadx/sources/p134n8/a.java), [`b.java`](re/masa/jadx/sources/p134n8/b.java) — Implementasi Java plugin `flutter_compass` pada MASA (`ROTATION_VECTOR`, fallback `alpha = 0.45f`, remap tilt `45°`).
- [`re/nu_online/disasm_qibla.txt`](re/nu_online/disasm_qibla.txt) — Disassembly ARM64 fungsi `QiblaUtil.calculateQiblaDirection` (`libapp.so` NU Online / `package:adhan`).
- [`re/nu_online/jadx/sources/R5/a.java`](re/nu_online/jadx/sources/R5/a.java), [`b.java`](re/nu_online/jadx/sources/R5/b.java) — Implementasi Java `flutter_device_compass` pada NU Online (`ROTATION_VECTOR`, fallback `alpha = 0.15f`, deadband `0.1°`).

---

## Build & Pengujian `qiblaku-app`

```bash
cd qiblaku-app
./gradlew testDebugUnitTest assembleDebug
```

---

## Lisensi

Seluruh kode sumber aplikasi, naskah PDF, dan catatan teknis pada repositori ini dirilis di bawah **MIT License** — lihat [`LICENSE`](LICENSE). Berkas biner aplikasi pihak ketiga yang dianalisis tetap menjadi hak milik pengembang masing-masing dan tidak didistribusikan dalam repositori ini.
