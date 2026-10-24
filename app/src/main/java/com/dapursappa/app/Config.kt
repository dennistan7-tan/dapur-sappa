package com.dapursappa.app

import java.security.MessageDigest

object Config {
    // Samakan dengan versionName di app/build.gradle.kts
    const val APP_VERSION = "0.4.13"

    // Kode toko disimpan sebagai hash.
    private const val CODE_SHA256 = "c90117a86df5ad6cff0574e82abc47165bc43d412365f33075da2a369c2c992f"

    // Password awal developer (hanya hash). Bisa diganti dari menu Kelola user.
    const val DEV_DEFAULT_USER = "developer"
    const val DEV_DEFAULT_HASH = "ab38eadaeb746599f2c1ee90f8267f31f467347462764a24d71ac1843ee77fe3"

    // Isi dari Firebase Console (lihat PANDUAN_FIREBASE.md) agar semua HP langsung tersambung.
    // Boleh dikosongkan; developer juga bisa mengisinya dari menu Kelola user di aplikasi.
    const val FIREBASE_PROJECT_ID = "dapur-sappa"
    const val FIREBASE_API_KEY = "AIzaSyBwAtVGZKGFfc_xihjCcP9_3gjc1SvTPhg"

    // GitHub: dipakai untuk cek update otomatis dan alamat versi web.
    // Username GitHub pemilik repo rilis.
    const val GITHUB_USER = "dennistan7-tan"
    const val GITHUB_REPO = "dapur-sappa"
    val updateEnabled: Boolean get() = GITHUB_USER.isNotEmpty()
    val WEB_URL: String get() = "https://$GITHUB_USER.github.io/$GITHUB_REPO/"

    fun sha256(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isCodeValid(code: String): Boolean = sha256(code) == CODE_SHA256
}
