package com.dapursappa.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dapursappa.app.R
import com.dapursappa.app.data.AppData
import com.dapursappa.app.data.Calc
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.ReportExport
import com.dapursappa.app.data.Xlsx
import java.io.File
import java.time.LocalDate

private val MON = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

/** Tanggal brosur, mis. "19 – 23 OCT 2026" (Senin sampai Jumat). */
fun brosurDate(mon: LocalDate): String {
    val fri = mon.plusDays(4)
    return if (mon.month == fri.month) "${mon.dayOfMonth} – ${fri.dayOfMonth} ${MON[fri.monthValue - 1]} ${fri.year}"
    else "${mon.dayOfMonth} ${MON[mon.monthValue - 1]} – ${fri.dayOfMonth} ${MON[fri.monthValue - 1]} ${fri.year}"
}

private fun brosurFont(ctx: Context): Typeface {
    // Pasang file font LAZYDOG di app/src/main/assets/fonts/lazydog.ttf (atau .otf)
    for (n in listOf("fonts/lazydog.ttf", "fonts/lazydog.otf", "fonts/LAZYDOG.ttf", "fonts/Lazydog.ttf")) {
        try { return Typeface.createFromAsset(ctx.assets, n) } catch (e: Exception) { }
    }
    return Typeface.create("sans-serif-black", Typeface.BOLD)
}

fun hasLazydog(ctx: Context): Boolean = listOf("lazydog.ttf", "lazydog.otf", "LAZYDOG.ttf", "Lazydog.ttf").any {
    try { ctx.assets.open("fonts/$it").close(); true } catch (e: Exception) { false }
}

/** Gambar brosur 1080x1920 dari template kosong + menu minggu itu. */
fun renderBrosur(ctx: Context, data: AppData, monday: LocalDate): Bitmap {
    val opt = BitmapFactory.Options().apply { inScaled = false; inMutable = true }
    val bmp = BitmapFactory.decodeResource(ctx.resources, R.drawable.brosur_template, opt)
        .copy(Bitmap.Config.ARGB_8888, true)
    val cv = Canvas(bmp)
    val w = bmp.width / 1080f
    val tf = brosurFont(ctx)
    val brown = 0xFF5A3A22.toInt()
    fun paint(size: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brown; typeface = tf; textSize = size * w; textAlign = Paint.Align.CENTER
    }
    // Tanggal
    run {
        val p = paint(54f)
        cv.drawText(brosurDate(monday), 540f * w, (480f + 18f) * w, p)
    }
    // Menu per hari (Senin-Jumat): pusat kolom dan pusat baris sesuai template
    val xs = listOf(195f, 545f, 903f, 195f, 545f)
    val ys = listOf(792f, 792f, 792f, 1130f, 1130f)
    for (i in 0 until 5) {
        val d = monday.plusDays(i.toLong())
        val dc = Calc.day(data, d)
        var lines = dc.menuName.lines().map { it.trim().uppercase() }.filter { it.isNotEmpty() }.take(3)
        if (lines.isEmpty() && dc.libur) lines = listOf("LIBUR")
        if (lines.isEmpty()) continue
        var size = 36f
        var p = paint(size)
        while (size > 18f && lines.any { p.measureText(it) > 345f * w }) { size -= 1f; p = paint(size) }
        val gap = 50f * w
        val n = lines.size
        for ((k, ln) in lines.withIndex()) {
            val base = ys[i] * w + (k - (n - 1) / 2f) * gap + size * w * 0.35f
            cv.drawText(ln, xs[i] * w, base, p)
        }
    }
    return bmp
}

private fun shareFile(ctx: Context, f: File, mime: String, title: String) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
    val i = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(i, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Simpan PNG ke galeri (Android 10+), lalu buka menu bagikan. Mengembalikan pesan status. */
fun saveAndShareBrosur(ctx: Context, bmp: Bitmap, monday: LocalDate): String {
    val name = "Menu-DapurSappa-${monday}.png"
    var msg = ""
    try {
        val dir = File(ctx.cacheDir, "exports"); dir.mkdirs()
        val f = File(dir, name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DapurSappa")
                }
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                if (uri != null) {
                    ctx.contentResolver.openOutputStream(uri)?.use { o -> f.inputStream().use { it.copyTo(o) } }
                    msg = "Tersimpan di Galeri (Pictures/DapurSappa)."
                }
            } catch (e: Exception) { }
        }
        shareFile(ctx, f, "image/png", "Bagikan brosur menu")
    } catch (e: Exception) {
        msg = "Gagal membuat brosur: ${e.message}"
    }
    return msg
}

/** Buat file Excel laporan lengkap lalu buka menu bagikan/simpan. */
fun exportLaporanExcel(ctx: Context, data: AppData): String {
    return try {
        val dir = File(ctx.cacheDir, "exports"); dir.mkdirs()
        val f = File(dir, "Laporan-DapurSappa-${LocalDate.now()}.xlsx")
        f.outputStream().use { Xlsx.write(it, ReportExport.sheets(data)) }
        shareFile(ctx, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Simpan / bagikan laporan")
        "Laporan Excel dibuat."
    } catch (e: Exception) {
        "Gagal export: ${e.message}"
    }
}

@Composable
fun BrosurScreen(data: AppData, onBack: () -> Unit) {
    val c = Sappa.c
    val ctx = LocalContext.current
    var week by remember { mutableStateOf(Fmt.mondayOf(LocalDate.now())) }
    var msg by remember { mutableStateOf("") }
    val bmp = remember(week, data) { renderBrosur(ctx, data, week) }
    val font = remember { hasLazydog(ctx) }

    Column(Modifier.fillMaxSize()) {
        SubHeader("Export brosur menu harian", "PNG 1080 × 1920, siap kirim", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NavBar("Week ${Fmt.weekNo(week)}", brosurDate(week), { week = week.minusWeeks(1); msg = "" }, { week = week.plusWeeks(1); msg = "" })
            Image(
                bitmap = bmp.asImageBitmap(), contentDescription = "Brosur menu",
                modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth,
            )
            if (!font) Muted("Font LAZYDOG belum terpasang, memakai font cadangan. Kirim file font ke developer untuk dipasang.", color = c.warn)
            if (msg.isNotEmpty()) Muted(msg, color = c.ok)
            PrimaryButton("Simpan dan bagikan PNG") { msg = saveAndShareBrosur(ctx, bmp, week) }
            Muted("Menu diambil dari Menu mingguan. Hari libur tampil LIBUR.")
        }
    }
}
