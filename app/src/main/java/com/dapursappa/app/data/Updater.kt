package com.dapursappa.app.data

import android.os.Handler
import android.os.Looper
import com.dapursappa.app.Config
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Cek versi terbaru di GitHub Releases. */
object Updater {
    data class Info(val version: String, val url: String, val notes: String)

    fun newer(remote: String, local: String): Boolean {
        val a = remote.trim().removePrefix("v").split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val b = local.trim().removePrefix("v").split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        for (i in 0 until Math.max(a.size, b.size)) {
            val x = if (i < a.size) a[i] else 0
            val y = if (i < b.size) b[i] else 0
            if (x != y) return x > y
        }
        return false
    }

    /** onDone(info, error): info != null jika ada versi baru; error != null jika gagal cek. */
    fun check(onDone: (Info?, String?) -> Unit) {
        if (!Config.updateEnabled) {
            onDone(null, "Alamat GitHub belum diatur")
            return
        }
        Thread {
            var info: Info? = null
            var err: String? = null
            try {
                val url = "https://api.github.com/repos/${Config.GITHUB_USER}/${Config.GITHUB_REPO}/releases/latest"
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                val code = c.responseCode
                if (code == 404) {
                    err = "Belum ada rilis di GitHub"
                } else if (code !in 200..299) {
                    err = "GitHub menjawab $code"
                } else {
                    val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                    val tag = j.optString("tag_name")
                    var apk = j.optString("html_url")
                    val assets = j.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val a = assets.getJSONObject(i)
                            if (a.optString("name").endsWith(".apk")) {
                                apk = a.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    if (newer(tag, Config.APP_VERSION)) {
                        info = Info(tag.removePrefix("v"), apk, j.optString("body").trim())
                    }
                }
                c.disconnect()
            } catch (e: Exception) {
                err = "Tidak bisa cek update (offline?)"
            }
            Handler(Looper.getMainLooper()).post { onDone(info, err) }
        }.start()
    }
}
