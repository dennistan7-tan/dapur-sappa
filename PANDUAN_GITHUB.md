# Panduan GitHub: versi web + update otomatis


## 1. Buat repository
1. github.com → tombol **New** → nama: `dapur-sappa` → pilih **Public** → Create repository.
2. Klik **uploading an existing file**, lalu seret SEMUA isi folder proyek ini (termasuk folder `.github`, `docs`, `app`, file `dapursappa.jks`). Commit.
   Folder `.github` tersembunyi di Windows: aktifkan File Explorer → View → Show → Hidden items.

## 2. Aktifkan versi web
Repo → Settings → Pages → Source: **Deploy from a branch** → Branch `main`, folder `/docs` → Save.
Beberapa menit kemudian web terbuka di `https://dennistan7-tan.github.io/dapur-sappa/`.

## 3. Rilis versi baru (memicu update di HP)
1. Naikkan `APP_VERSION` di `Config.kt` dan `versionCode`/`versionName` di `app/build.gradle.kts`, tulis catatan di `release-notes.txt`, upload ke GitHub.
2. Repo → Releases → **Create a new release** → buat tag baru, mis. `v0.3.1` → Publish.
3. Tunggu ±5 menit: GitHub Actions membuat APK dan melampirkannya ke rilis itu.
4. Aplikasi di HP akan menampilkan "Ada versi baru"; tekan **Update sekarang**, unduh, lalu pasang.

Catatan: APK dibuat dengan kunci tanda tangan yang sama (`dapursappa.jks`), jadi bisa dipasang menimpa versi lama. Repo public berarti kunci ini terlihat publik; untuk pemakaian pribadi ini dapat diterima.
