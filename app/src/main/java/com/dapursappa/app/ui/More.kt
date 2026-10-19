package com.dapursappa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapursappa.app.Config
import com.dapursappa.app.data.AppData
import com.dapursappa.app.data.Calc
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Menu
import com.dapursappa.app.data.Prefs
import com.dapursappa.app.data.Repo
import com.dapursappa.app.data.Sync
import com.dapursappa.app.data.UserCfg
import com.dapursappa.app.data.Updater
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

@Composable
fun MoreTab(
    me: Me,
    prefs: Prefs,
    onSwitchUser: () -> Unit,
    onMenu: () -> Unit,
    onRiwayat: () -> Unit,
    onKelola: () -> Unit,
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    onBrosur: () -> Unit,
) {
    val c = Sappa.c
    val ctx = LocalContext.current
    var updMsg by remember { mutableStateOf("") }
    var updInfo by remember { mutableStateOf<Updater.Info?>(null) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Menu mingguan dan harga: paling atas dan disorot
        val hs = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
        Row(
            Modifier.fillMaxWidth()
                .clip(hs).background(c.menuBg).border(2.dp, c.accent, hs)
                .clickable(onClick = onMenu).padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Menu mingguan dan harga", color = c.accentText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("Atur menu tiap hari dan harga porsi per minggu", color = c.text2, fontSize = 12.sp)
            }
            Text("›", color = c.accentText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
        SCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Strong(me.name, size = 18)
                    Muted("${me.label} · di HP ini")
                }
                TextButton(onClick = onSwitchUser) { Text("Ganti user", color = c.accentText, fontWeight = FontWeight.ExtraBold) }
            }
        }
        MoreItem("Export brosur menu harian", "PNG", onBrosur)
        SCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Strong("Tampilan", size = 15)
                    Muted(if (dark) "Mode gelap" else "Mode terang")
                }
                ThemeSwitch(dark, onDark)
            }
        }
        MoreItem("Riwayat perubahan", null, onRiwayat)
        if (me.isDev) MoreItem("Kelola user", "Developer", onKelola)

        SectionTitle("Aplikasi")
        SCard {
            Strong("Dapur Sappa · versi ${Config.APP_VERSION}")
            if (updMsg.isNotEmpty()) Muted(updMsg, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            OutlineButton("Cek update") {
                updMsg = "Mengecek..."
                Updater.check { info, err ->
                    if (info != null) { updInfo = info; updMsg = "Versi ${info.version} tersedia." }
                    else updMsg = err ?: "Sudah versi terbaru."
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlineButton("Buka versi web (komputer)") {
                if (Config.updateEnabled) openUrl(ctx, Config.WEB_URL) else updMsg = "Alamat web belum diatur."
            }
        }
        val ui = updInfo
        if (ui != null) UpdateDialog(ui) { updInfo = null }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MoreItem(title: String, tag: String?, onClick: () -> Unit) {
    val c = Sappa.c
    ClickCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Strong(title, size = 15)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tag != null) { Tag(tag, c.accentSoft, c.accentText); Spacer(Modifier.width(8.dp)) }
                Text("›", color = c.text2, fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun MenuScreen(me: Me, data: AppData, onBack: () -> Unit) {
    val c = Sappa.c
    var week by remember { mutableStateOf(Fmt.mondayOf(LocalDate.now())) }
    val saved = data.menus[week.toString()]
    val base = saved ?: Menu.empty(week.toString(), Calc.defaultPrice(data, week))
    // Tiap hari punya 3 baris isian menu (disimpan digabung dengan newline).
    val lines = remember(week, saved) {
        mutableStateListOf<String>().also { l ->
            for (i in 0 until 7) {
                val p = base.days[i].lines().map { it.trim() }.filter { it.isNotEmpty() }
                for (j in 0 until 3) l.add(p.getOrElse(j) { "" })
            }
        }
    }
    val libur = remember(week, saved) { mutableStateListOf<Boolean>().also { it.addAll(base.libur) } }
    var price by remember(week, saved) { mutableStateOf(base.price.toString()) }
    var savedMsg by remember { mutableStateOf("") }
    val editable = me.canAdmin

    Column(Modifier.fillMaxSize()) {
        SubHeader("Menu mingguan", "Diatur admin, tampil di semua layar", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NavBar(
                "Week ${Fmt.weekNo(week)}", Fmt.range(week, week.plusDays(6)),
                { week = week.minusWeeks(1); savedMsg = "" }, { week = week.plusWeeks(1); savedMsg = "" },
            )
            SCard {
                for (i in 0 until 7) {
                    if (i > 0) Rule()
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.width(58.dp)) {
                            Strong(Fmt.dayName(week.plusDays(i.toLong())), size = 13)
                            Muted(Fmt.dayMon(week.plusDays(i.toLong())), size = 11)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val hints = listOf("Nasi / karbo", "Lauk utama", "Sayur / pendamping")
                            for (j in 0 until 3) {
                                Field(
                                    if (j == 0) (if (libur[i]) "Libur" else "Menu") else "", lines[i * 3 + j], { lines[i * 3 + j] = it },
                                    Modifier.fillMaxWidth(), placeholder = if (j == 0 && i == 6) "Menu PO spesial" else hints[j], enabled = editable,
                                )
                            }
                        }
                        Chip("Libur", libur[i]) { if (editable) libur[i] = !libur[i] }
                    }
                }
            }
            Banner("Minggu biasanya libur", "Untuk PO menu spesial, matikan Libur dan isi menunya. Hari itu tampil seperti hari biasa.")
            Field("Harga jual per porsi (Rp)", price, { price = digits(it) }, keyboard = androidx.compose.ui.text.input.KeyboardType.Number, enabled = editable)
            Muted("Harga bisa diubah per order saat mencatat order.")
            if (!editable) Muted("Hanya admin yang bisa mengubah menu.", color = c.warn)
            if (savedMsg.isNotEmpty()) Muted(savedMsg, color = c.ok)
            if (editable) {
                PrimaryButton("Simpan menu") {
                    val p = price.toLongOrNull() ?: 20000L
                    val dayTexts = List(7) { i -> (0 until 3).map { lines[i * 3 + it].trim() }.filter { it.isNotEmpty() }.joinToString("\n") }
                    Repo.saveMenu(Menu(week.toString(), dayTexts, libur.toList(), p), saved, me)
                    savedMsg = "Menu tersimpan."
                }
                OutlineButton("Salin dari minggu lalu") {
                    val prev = data.menus[week.minusWeeks(1).toString()]
                    if (prev != null) {
                        for (i in 0 until 7) {
                            val p = prev.days[i].lines().map { it.trim() }.filter { it.isNotEmpty() }
                            for (j in 0 until 3) lines[i * 3 + j] = p.getOrElse(j) { "" }
                            libur[i] = prev.libur[i]
                        }
                        price = prev.price.toString()
                        savedMsg = "Disalin. Tekan Simpan menu untuk menyimpan."
                    } else savedMsg = "Minggu lalu belum ada menu."
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun RiwayatScreen(data: AppData, onBack: () -> Unit) {
    val c = Sappa.c
    val filters = listOf("Semua", "Belanja", "Order", "Pengeluaran", "Menu", "User")
    var filter by remember { mutableStateOf("Semua") }
    val list = data.audits.filter { filter == "Semua" || it.cat == filter }.take(300)
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize()) {
        SubHeader("Riwayat perubahan", "Siapa mengubah apa, dan kapan", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChipScroll { filters.forEach { f -> Chip(f, filter == f) { filter = f } } }
            if (list.isEmpty()) Muted("Belum ada riwayat.")
            var lastDay: LocalDate? = null
            for (a in list) {
                val d = Fmt.dateOf(a.ts)
                if (d != lastDay) {
                    lastDay = d
                    val label = when (d) {
                        today -> "Hari ini"
                        today.minusDays(1) -> "Kemarin"
                        else -> Fmt.dayLong(d)
                    }
                    Strong(label + (if (d == today || d == today.minusDays(1)) " · " + Fmt.dayShort(d) else ""), size = 13, modifier = Modifier.padding(top = 6.dp))
                }
                SCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Strong(a.title, size = 14, modifier = Modifier.weight(1f))
                        Muted(Fmt.time(a.ts))
                    }
                    Muted(a.who)
                    if (a.detail.isNotBlank()) Text(a.detail, color = c.text, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Muted("Data yang dihapus tetap tercatat di sini.")
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun KelolaUserScreen(me: Me, data: AppData, prefs: Prefs, onBack: () -> Unit) {
    val c = Sappa.c
    val names = remember(data.users) { mutableStateListOf<String>().also { l -> data.users.forEach { l.add(it.name) } } }
    var project by remember { mutableStateOf(prefs.fbProject) }
    var key by remember { mutableStateOf(prefs.fbKey) }
    var showPass by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val status by Sync.status.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SubHeader("Kelola user", "Hanya developer", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Muted("Semua user punya akses penuh (belanja, order, menu, laporan).")
            for (i in names.indices) {
                SCard {
                    Field("Nama User ${i + 1}", names[i], { names[i] = it.take(24) })
                }
            }

            SectionTitle("Keamanan")
            SCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Strong("Kode toko")
                        Muted("5 digit · diminta sekali per HP")
                    }
                    Strong("•••••")
                }
            }
            SCard {
                Strong("Akun developer")
                Muted("Masuk hanya dengan password", modifier = Modifier.padding(bottom = 8.dp))
                OutlineButton("Ganti password developer") { showPass = true }
            }

            SectionTitle("Sinkron online (Firebase)")
            SCard {
                Muted("Isi dari Firebase Console (lihat PANDUAN_FIREBASE.md). Berlaku di HP ini; HP lain bisa diisi sama atau ditanam di Config.kt.", modifier = Modifier.padding(bottom = 8.dp))
                Field("Project ID", project, { project = it })
                Spacer(Modifier.height(8.dp))
                Field("Web API key", key, { key = it })
                Spacer(Modifier.height(8.dp))
                Muted("Status: $status")
            }

            if (msg.isNotEmpty()) Muted(msg, color = c.ok)
            PrimaryButton("Simpan perubahan") {
                val list = names.indices.map { UserCfg(names[it].ifBlank { "User ${it + 1}" }, "User") }
                if (list != data.users) Repo.saveUsers(list, me)
                prefs.fbProject = project
                prefs.fbKey = key
                Sync.kick()
                msg = "Tersimpan."
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPass) {
        var p1 by remember { mutableStateOf("") }
        var p2 by remember { mutableStateOf("") }
        var err by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPass = false },
            containerColor = c.surface,
            title = { Text("Ganti password developer", color = c.text, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field("Password baru", p1, { p1 = it; err = "" }, password = true)
                    Field("Ulangi password", p2, { p2 = it; err = "" }, password = true)
                    if (err.isNotEmpty()) Muted(err, color = c.accentText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (p1.length < 6) err = "Minimal 6 karakter."
                    else if (p1 != p2) err = "Password tidak sama."
                    else {
                        prefs.devHash = Config.sha256(p1)
                        msg = "Password developer diganti (berlaku di HP ini)."
                        showPass = false
                    }
                }) { Text("Simpan", color = c.accentText, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showPass = false }) { Text("Batal", color = c.text2) } },
        )
    }
}

/** Toggle LIGHT / DARK: bisa digeser (swipe) atau diketuk. */
@Composable
fun ThemeSwitch(dark: Boolean, onChange: (Boolean) -> Unit) {
    val c = Sappa.c
    val w = 92.dp
    val h = 38.dp
    val thumb = 32.dp
    val pad = 3.dp
    val x by androidx.compose.animation.core.animateDpAsState(if (dark) w - thumb - pad else pad, label = "thumb")
    var drag by remember { mutableStateOf(0f) }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
    Box(
        Modifier.width(w).height(h).clip(shape)
            .background(if (dark) c.accentSoft else c.surface2)
            .border(1.5.dp, c.border, shape)
            .clickable { onChange(!dark) }
            .pointerInput(dark) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (drag > 8f && !dark) onChange(true) else if (drag < -8f && dark) onChange(false) },
                ) { _, amount -> drag += amount }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            if (dark) "DARK" else "LIGHT",
            modifier = Modifier.align(if (dark) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 12.dp),
            color = c.text2, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp,
        )
        Box(
            Modifier.offset(x = x).size(thumb).clip(androidx.compose.foundation.shape.CircleShape).background(c.accent),
        )
    }
}
