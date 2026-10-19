package com.dapursappa.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dapursappa.app.Config
import com.dapursappa.app.data.Updater
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // abaikan jika tidak ada browser
    }
}

/** Unduh APK ke cache aplikasi lalu buka layar Pasang. onState: teks status, onProgress: 0..100. */
fun downloadAndInstall(ctx: Context, url: String, onProgress: (Int) -> Unit, onDone: (String?) -> Unit) {
    val main = Handler(Looper.getMainLooper())
    Thread {
        var err: String? = null
        try {
            val dir = File(ctx.cacheDir, "updates")
            dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() }
            val f = File(dir, "DapurSappa-update.apk")
            var conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            var hops = 0
            while (conn.responseCode in 300..399 && hops < 5) {
                val loc = conn.getHeaderField("Location") ?: break
                conn.disconnect()
                conn = URL(loc).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                hops++
            }
            if (conn.responseCode !in 200..299) {
                err = "Unduh gagal (kode ${conn.responseCode})"
            } else {
                val total = conn.contentLength.toLong()
                var got = 0L
                var last = -1
                conn.inputStream.use { inp ->
                    f.outputStream().use { out ->
                        val buf = ByteArray(32 * 1024)
                        while (true) {
                            val n = inp.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            got += n
                            if (total > 0) {
                                val pct = (got * 100 / total).toInt()
                                if (pct != last) { last = pct; main.post { onProgress(pct) } }
                            }
                        }
                    }
                }
                conn.disconnect()
                main.post {
                    try {
                        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                        val i = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(i)
                    } catch (e: Exception) {
                        onDone("Gagal membuka pemasang: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            err = "Unduh gagal (offline?)"
        }
        main.post { onDone(err) }
    }.start()
}

@Composable
fun UpdateDialog(info: Updater.Info, onDismiss: () -> Unit) {
    val c = Sappa.c
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var pct by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = c.surface,
        title = { Text("Update tersedia", color = c.text, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Versi ${info.version} sudah ada (di HP ini: ${Config.APP_VERSION}).", color = c.text)
                if (info.notes.isNotBlank()) {
                    Text(info.notes.take(600), color = c.text2, modifier = Modifier.padding(top = 8.dp))
                }
                Text(
                    if (busy) "Mengunduh... $pct%" else
                        "Tekan Update: APK diunduh otomatis lalu tekan Pasang. Data tidak hilang.",
                    color = c.text2, modifier = Modifier.padding(top = 8.dp),
                )
                if (msg.isNotBlank()) Text(msg, color = c.text, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
                    msg = "Aktifkan dulu izin \"Izinkan dari sumber ini\" untuk Dapur Sappa, lalu kembali dan tekan Update lagi."
                    try {
                        ctx.startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) { }
                } else {
                    busy = true; msg = ""; pct = 0
                    downloadAndInstall(ctx, info.url, { pct = it }) { e ->
                        busy = false
                        if (e != null) msg = e else onDismiss()
                    }
                }
            }) {
                Text(if (busy) "Mengunduh..." else "Update sekarang", color = c.accentText, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Nanti", color = c.text2) } },
    )
}
