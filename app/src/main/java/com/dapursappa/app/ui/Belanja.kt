package com.dapursappa.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapursappa.app.data.AppData
import com.dapursappa.app.data.Calc
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.Kind
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Parsed
import com.dapursappa.app.data.Parser
import com.dapursappa.app.data.Purchase
import com.dapursappa.app.data.Repo
import com.dapursappa.app.data.Store
import java.time.LocalDate

fun menuBanner(data: AppData, date: LocalDate): String {
    val mn = Calc.menuName(data, date)
    val libur = Calc.isLibur(data, date)
    val label = when {
        mn.isNotBlank() -> mn
        libur -> "Libur"
        else -> "Menu belum diisi"
    }
    return Fmt.dayLong(date) + " · " + label
}

@Composable
fun BelanjaTab(me: Me, data: AppData) {
    val c = Sappa.c
    val today = LocalDate.now()
    var viewDate by remember { mutableStateOf(today) }
    var text by remember { mutableStateOf("") }
    var kindOverride by remember { mutableStateOf<Kind?>(null) }
    var useDate by remember { mutableStateOf(today) }
    var pickDate by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Purchase?>(null) }
    var dialogNew by remember { mutableStateOf(false) }

    val parsed = remember(text) { Parser.parse(text) }
    val kind = kindOverride ?: parsed?.kind ?: Kind.BAHAN
    val items = data.purchases.filter { it.date == viewDate.toString() }
    val forToday = items.filter { it.kind != Kind.BULANAN && it.useDate == it.date }.sumOf { it.price }
    val forLater = items.filter { it.kind == Kind.BAHAN && it.useDate > it.date }.sumOf { it.price }
    val monthly = items.filter { it.kind == Kind.BULANAN }.sumOf { it.price }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Banner("Menu hari ini", menuBanner(data, today))

        SCard {
            Strong("Catat belanja", size = 16)
            Muted("Ketik bebas, urutan boleh terbalik. Contoh: ayam dada fillet 36rb 1kg", modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            Field(
                "Belanja", text, { text = it; kindOverride = null },
                placeholder = "selada 3 ikat 9rb",
            )
            if (parsed != null) {
                Spacer(Modifier.height(10.dp))
                if (parsed.ok) {
                    PreviewBlock(parsed, kind)
                    Spacer(Modifier.height(10.dp))
                    Muted("Jenis")
                    Spacer(Modifier.height(4.dp))
                    ChipScroll {
                        Kind.values().forEach { k -> Chip(k.label, kind == k) { kindOverride = k } }
                    }
                    if (kind == Kind.BAHAN) {
                        Spacer(Modifier.height(10.dp))
                        Muted("Dipakai untuk tanggal")
                        Spacer(Modifier.height(4.dp))
                        ChipScroll {
                            Chip("Hari ini", useDate == today) { useDate = today }
                            Chip("Besok", useDate == today.plusDays(1)) { useDate = today.plusDays(1) }
                            Chip(
                                if (useDate != today && useDate != today.plusDays(1)) Fmt.dayShort(useDate) else "Pilih tanggal",
                                useDate != today && useDate != today.plusDays(1),
                            ) { pickDate = true }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton("Ubah", Modifier.weight(1f)) {
                            dialogNew = true
                            dialog = draftFrom(parsed, text, kind, viewDate, if (kind == Kind.BAHAN) useDate else viewDate, me)
                        }
                        PrimaryButton("Simpan", Modifier.weight(1f)) {
                            val p = draftFrom(parsed, text, kind, viewDate, if (kind == Kind.BAHAN) useDate else viewDate, me)
                            Repo.savePurchase(p, null, me)
                            text = ""; kindOverride = null; useDate = today
                        }
                    }
                } else {
                    Muted(
                        if (parsed.price <= 0) "Harga belum terbaca. Tulis harga seperti 36rb atau 36000." else "Nama barang belum terbaca.",
                        color = c.warn,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlineButton("Isi manual") {
                        dialogNew = true
                        dialog = draftFrom(parsed, text, kind, viewDate, viewDate, me)
                    }
                }
            }
        }

        NavBar(
            if (viewDate == today) "Belanja hari ini" else "Belanja",
            Fmt.dayLong(viewDate) + if (canEdit(me, viewDate.toString())) " · bisa diedit" else " · hanya lihat",
            { viewDate = viewDate.minusDays(1) }, { viewDate = viewDate.plusDays(1) },
        )

        if (items.isEmpty()) {
            Muted("Belum ada belanja di tanggal ini.", modifier = Modifier.padding(vertical = 8.dp))
        }
        for (p in items) {
            ClickCard(onClick = {
                if (canEdit(me, p.date)) { dialogNew = false; dialog = p }
            }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Strong(p.name, size = 15)
                        Muted(purchaseSub(p))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Strong(Fmt.rp(p.price), size = 15)
                        kindTag(p.kind)
                    }
                }
            }
        }

        SCard(background = c.surface2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Muted("Untuk hari ini", size = 13); Strong(Fmt.rp(forToday))
            }
            if (forLater > 0) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Muted("Untuk hari lain", size = 13); Strong(Fmt.rp(forLater))
                }
            }
            if (monthly > 0) {
                Spacer(Modifier.height(6.dp))
                Muted("Biaya bulanan ${Fmt.rp(monthly)} masuk ke profit bulanan, tidak ke profit harian.")
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (pickDate) DatePickDialog(useDate, { useDate = it }, { pickDate = false })

    val d = dialog
    if (d != null) {
        PurchaseDialog(
            init = d, isNew = dialogNew,
            onSave = { np ->
                Repo.savePurchase(np, if (dialogNew) null else d, me)
                if (dialogNew) { text = ""; kindOverride = null; useDate = today }
                dialog = null
            },
            onDelete = if (dialogNew) null else ({ Repo.deletePurchase(d, me); dialog = null }),
            onDismiss = { dialog = null },
        )
    }
}

fun purchaseSub(p: Purchase): String {
    val parts = ArrayList<String>()
    parts.add((Fmt.num(p.qty) + " " + p.unit).trim())
    if (p.kind == Kind.KEMASAN && p.qty > 0) parts.add(Fmt.rp(Math.round(p.price / p.qty)) + " per " + p.unit.ifBlank { "pcs" })
    if (p.kind == Kind.BAHAN && p.useDate != p.date) {
        parts.add("untuk " + Fmt.dayName(LocalDate.parse(p.useDate)))
    }
    return parts.joinToString(" · ")
}

@Composable
fun kindTag(k: Kind) {
    val c = Sappa.c
    when (k) {
        Kind.BAHAN -> Tag(k.label, c.okSoft, c.ok)
        Kind.KEMASAN -> Tag(k.label, c.accentSoft, c.accentText)
        Kind.BULANAN -> Tag(k.label, c.warnSoft, c.warn)
    }
}

fun draftFrom(p: Parsed, raw: String, kind: Kind, date: LocalDate, use: LocalDate, me: Me) = Purchase(
    id = Store.newId(), date = date.toString(), useDate = use.toString(), raw = raw,
    name = p.name, qty = p.qty, unit = p.unit, price = p.price, kind = kind, by = me.idx,
)

@Composable
private fun PreviewBlock(p: Parsed, kind: Kind) {
    val c = Sappa.c
    Column(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text("Terbaca otomatis", color = c.ok, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Strong(p.name + " · " + (Fmt.num(p.qty) + " " + p.unit).trim() + " · " + Fmt.rp(p.price), size = 15)
        val per = if (p.qty > 0) Math.round(p.price / p.qty) else p.price
        val perTxt = if (p.unit == "kg") "Rp" + Fmt.num(p.price / (p.qty * 1000)) + " per gram" else Fmt.rp(per) + " per " + p.unit.ifBlank { "satuan" }
        Muted("Jenis: ${kind.label.lowercase()} · $perTxt")
    }
}

@Composable
fun PurchaseDialog(
    init: Purchase,
    isNew: Boolean,
    onSave: (Purchase) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val c = Sappa.c
    var name by remember { mutableStateOf(init.name) }
    var qty by remember { mutableStateOf(Fmt.num(init.qty)) }
    var unit by remember { mutableStateOf(init.unit) }
    var price by remember { mutableStateOf(init.price.toString()) }
    var kind by remember { mutableStateOf(init.kind) }
    var use by remember { mutableStateOf(LocalDate.parse(init.useDate)) }
    var pick by remember { mutableStateOf(false) }
    val base = LocalDate.parse(init.date)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(if (isNew) "Ubah sebelum simpan" else "Edit belanja", color = c.text, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Nama barang", name, { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Jumlah", qty, { qty = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }.take(8) }, Modifier.weight(1f), KeyboardType.Decimal)
                    Field("Satuan", unit, { unit = it.take(12) }, Modifier.weight(1f))
                }
                Field("Harga total (Rp)", price, { price = digits(it) }, keyboard = KeyboardType.Number)
                Muted("Jenis")
                ChipScroll { Kind.values().forEach { k -> Chip(k.label, kind == k) { kind = k } } }
                if (kind == Kind.BAHAN) {
                    Muted("Dipakai untuk tanggal")
                    ChipScroll {
                        Chip("Hari beli", use == base) { use = base }
                        Chip("Besok", use == base.plusDays(1)) { use = base.plusDays(1) }
                        Chip(if (use != base && use != base.plusDays(1)) Fmt.dayShort(use) else "Pilih tanggal", use != base && use != base.plusDays(1)) { pick = true }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pr = price.toLongOrNull() ?: 0L
                if (name.isNotBlank() && pr > 0) {
                    val q = qty.replace(',', '.').toDoubleOrNull() ?: 1.0
                    onSave(init.copy(name = name.trim(), qty = q, unit = unit.trim(), price = pr, kind = kind, useDate = if (kind == Kind.BAHAN) use.toString() else init.date))
                }
            }) { Text("Simpan", color = c.accentText, fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Hapus", color = c.accentText) }
                TextButton(onClick = onDismiss) { Text("Batal", color = c.text2) }
            }
        },
    )
    if (pick) DatePickDialog(use, { use = it }, { pick = false })
}
