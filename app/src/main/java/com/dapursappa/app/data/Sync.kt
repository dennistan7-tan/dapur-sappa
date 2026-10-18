package com.dapursappa.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Sinkron ke Firestore lewat REST (tanpa SDK). Login anonim.
 * Data: shops/dapursappa/records/{id} dengan field d (JSON) dan u (waktu ubah).
 * Aturan Firestore: allow read, write: if request.auth != null
 */
object Sync {
    private val queue = LinkedBlockingQueue<Int>()
    private var started = false
    private var prefs: Prefs? = null
    private var token: String? = null
    private var tokenExp: Long = 0L

    private val st = MutableStateFlow("Belum diatur")
    val status: StateFlow<String> = st
    private val ls = MutableStateFlow(0L)
    val lastSync: StateFlow<Long> = ls

    fun kick() {
        queue.offer(1)
    }

    fun start(p: Prefs) {
        prefs = p
        ls.value = p.lastSync
        if (started) return
        started = true
        val t = Thread {
            while (true) {
                try {
                    runOnce()
                } catch (e: Exception) {
                    st.value = "Offline · " + (e.message ?: "gagal terhubung").take(60)
                }
                try {
                    queue.poll(15, TimeUnit.SECONDS)
                    queue.clear()
                } catch (e: InterruptedException) {
                    return@Thread
                }
            }
        }
        t.isDaemon = true
        t.start()
    }

    private fun http(method: String, url: String, body: String?, bearer: String?): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 10000
        c.readTimeout = 15000
        c.setRequestProperty("Content-Type", "application/json")
        if (bearer != null) c.setRequestProperty("Authorization", "Bearer $bearer")
        if (body != null) {
            c.doOutput = true
            OutputStreamWriter(c.outputStream, "UTF-8").use { it.write(body) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        c.disconnect()
        return Pair(code, text)
    }

    private fun ensureToken(key: String): String {
        val now = System.currentTimeMillis()
        val t = token
        if (t != null && now < tokenExp - 300_000) return t
        val (code, text) = http(
            "POST", "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$key",
            "{\"returnSecureToken\":true}", null,
        )
        if (code !in 200..299) {
            throw RuntimeException("Login anonim gagal ($code). Aktifkan Anonymous di Firebase Auth dan cek API key.")
        }
        val j = JSONObject(text)
        token = j.getString("idToken")
        tokenExp = now + j.optLong("expiresIn", 3600L) * 1000L
        return token!!
    }

    private fun runOnce() {
        val p = prefs ?: return
        val project = p.fbProject
        val key = p.fbKey
        if (project.isBlank() || key.isBlank()) {
            st.value = "Belum diatur (mode lokal)"
            return
        }
        val tk = ensureToken(key)
        val base = "https://firestore.googleapis.com/v1/projects/$project/databases/(default)/documents"

        // 1) kirim perubahan lokal
        for (o in Store.dirty()) {
            val id = o.getString("id")
            val u = o.optLong("u")
            o.remove("_d")
            val body = JSONObject().put(
                "fields",
                JSONObject()
                    .put("d", JSONObject().put("stringValue", o.toString()))
                    .put("u", JSONObject().put("integerValue", u.toString())),
            ).toString()
            val url = "$base/shops/dapursappa/records/$id?updateMask.fieldPaths=d&updateMask.fieldPaths=u"
            val (code, text) = http("PATCH", url, body, tk)
            if (code !in 200..299) throw RuntimeException("Kirim gagal ($code) ${text.take(80)}")
            Store.markPushed(id, u)
        }

        // 2) ambil perubahan dari server
        val since = Math.max(0L, p.lastPull - 120_000L)
        val query = JSONObject().put(
            "structuredQuery",
            JSONObject()
                .put("from", JSONArray().put(JSONObject().put("collectionId", "records")))
                .put(
                    "where",
                    JSONObject().put(
                        "fieldFilter",
                        JSONObject()
                            .put("field", JSONObject().put("fieldPath", "u"))
                            .put("op", "GREATER_THAN")
                            .put("value", JSONObject().put("integerValue", since.toString())),
                    ),
                ),
        ).toString()
        val (code, text) = http("POST", "$base/shops/dapursappa:runQuery", query, tk)
        if (code !in 200..299) throw RuntimeException("Ambil gagal ($code) ${text.take(80)}")
        val arr = JSONArray(text)
        val remote = ArrayList<JSONObject>()
        var maxU = p.lastPull
        for (i in 0 until arr.length()) {
            val doc = arr.getJSONObject(i).optJSONObject("document") ?: continue
            val fields = doc.optJSONObject("fields") ?: continue
            val d = fields.optJSONObject("d")?.optString("stringValue") ?: continue
            try {
                val rec = JSONObject(d)
                rec.put("u", fields.optJSONObject("u")?.optString("integerValue")?.toLongOrNull() ?: rec.optLong("u"))
                remote.add(rec)
                if (rec.optLong("u") > maxU) maxU = rec.optLong("u")
            } catch (e: Exception) {
                // lewati
            }
        }
        if (remote.isNotEmpty()) Store.merge(remote)
        p.lastPull = maxU
        val now = System.currentTimeMillis()
        p.lastSync = now
        ls.value = now
        st.value = "Online"
    }
}
