# Panduan Firebase untuk Dapur Sappa

Aplikasi jalan penuh tanpa Firebase (data tersimpan di HP). Agar 4 HP saling sinkron,
aplikasi butuh **Project ID** dan **Web API key** dari Firebase. File `google-services.json`
**tidak diperlukan**.

## 1. Buat project Firebase
1. Buka https://console.firebase.google.com lalu login dengan akun Google.
2. **Create a project** → nama `dapur-sappa` → Analytics boleh dimatikan → Create.

## 2. Daftarkan aplikasi Android
1. Di halaman project klik ikon **Android** (Add app).
2. **Android package name**: `com.dapursappa.app` (harus persis sama).
3. Nickname bebas, SHA-1 dikosongkan dulu → **Register app**.
4. Langkah download `google-services.json` boleh dilewati.
5. Catat **Project ID** dan **Web API key**: klik ikon roda gigi > **Project settings** > tab General.
   Project ID ada di bagian atas, Web API key ada di bagian "Web API Key" (jika kosong, aktifkan Authentication dulu).

## 3. Aktifkan Firestore
1. **Build > Firestore Database > Create database**.
2. Lokasi: `asia-southeast2` (Jakarta) atau `asia-southeast1` (Singapore). Tidak bisa diubah nanti.
3. Pilih **Production mode**.

## 4. Aktifkan Authentication anonim
1. **Build > Authentication > Get started**.
2. Tab **Sign-in method** → **Anonymous** → Enable → Save.

## 5. Rules Firestore (sementara)
**Firestore Database > Rules**, tempel lalu **Publish**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /shops/{shop}/{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```
Rules ini akan diperketat setelah struktur data final.

## 6. Sambungkan aplikasi
Pilih salah satu:
- **Dari aplikasi**: login sebagai developer > Lainnya > Kelola user > isi **Project ID** dan **Web API key** > Simpan perubahan. Ulangi di tiap HP.
- **Tanam di kode** (sekali untuk semua HP): isi `FIREBASE_PROJECT_ID` dan `FIREBASE_API_KEY` di `Config.kt`, lalu build ulang.

Status di menu Lainnya akan berubah menjadi **Online**. Data dari semua HP digabung otomatis tiap beberapa detik.
