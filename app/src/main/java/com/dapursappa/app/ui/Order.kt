package com.dapursappa.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.dapursappa.app.data.Extra
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Order
import com.dapursappa.app.data.Repo
import com.dapursappa.app.data.Store
import java.time.LocalDate

@Composable
fun OrderTab(
    me: Me,
    data: AppData,
    date: LocalDate,
    onDate: (LocalDate) -> Unit,
    onOpen: (String?, LocalDate) -> Unit,
) {
    val c = Sappa.c
    val ds = date.toString()
    val orders = data.orders.filter { it.date == ds }.reversed()
    val pax = orders.sumOf { it.portions }
    val omzet = orders.sumOf { it.total }
    val unpaid = orders.filter { !it.paid && it.total > 0 }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Banner(if (date == LocalDate.now()) "Menu hari ini" else "Menu", menuBanner(data, date))
        NavBar("Order", Fmt.dayLong(date), { onDate(date.minusDays(1)) }, { onDate(date.plusDays(1)) })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Order", orders.size.toString(), Modifier.weight(1f))
            StatTile("Porsi", pax.toString(), Modifier.weight(1f))
            StatTile("Omzet", Fmt.rp(omzet), Modifier.weight(1.4f), highlight = true)
        }

        SectionTitle("Daftar order")
        if (orders.isEmpty()) Muted("Belum ada order di tanggal ini.", modifier = Modifier.padding(vertical = 8.dp))
        for (o in orders) {
            ClickCard(onClick = { onOpen(o.id, date) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Strong(o.customer, size = 15)
                    Strong(Fmt.rp(o.total), size = 15)
                }
                val sub = (if (o.location.isBlank()) "" else o.location + " · ") + (if (o.onlyExtra) "tanpa porsi" else "${o.pax} pax")
                Muted(sub)
                if (o.remarks.isNotBlank()) Muted(o.remarks, modifier = Modifier.padding(top = 2.dp))
                for (e in o.extras) {
                    Muted("Tambahan: ${e.name} ${e.qty} × ${Fmt.rp(e.unitPrice)}", modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (o.onlyExtra) Tag("Lauk saja", c.accentSoft, c.accentText)
                    else if (o.extrasTotal > 0) Tag("+ Tambahan ${Fmt.rp(o.extrasTotal)}", c.accentSoft, c.accentText)
                    if (o.paid) Tag("Lunas", c.okSoft, c.ok) else Tag("Belum bayar", c.warnSoft, c.warn)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { Repo.saveOrder(o.copy(paid = !o.paid), o, me) }) {
                        Text(if (o.paid) "Tandai belum" else "Tandai lunas", color = c.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (unpaid.isNotEmpty()) {
            SCard(background = c.warnSoft) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Strong("Belum dibayar", color = c.warn)
                    Strong(Fmt.rp(unpaid.sumOf { it.total }), color = c.warn)
                }
                Muted("${unpaid.size} order belum lunas", color = c.warn)
            }
        }
        PrimaryButton("+  Tambah order") { onOpen(null, date) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun OrderForm(me: Me, data: AppData, orderId: String?, date: LocalDate, onClose: () -> Unit) {
    val c = Sappa.c
    val old = remember(orderId) { data.orders.firstOrNull { it.id == orderId } }
    var dt by remember { mutableStateOf(old?.let { LocalDate.parse(it.date) } ?: date) }
    var customer by remember { mutableStateOf(old?.customer ?: "") }
    var location by remember { mutableStateOf(old?.location ?: "") }
    var pax by remember { mutableStateOf(old?.pax ?: 1) }
    var price by remember { mutableStateOf((old?.portionPrice ?: Calc.defaultPrice(data, date)).toString()) }
    val extras = remember { mutableStateListOf<Extra>().also { l -> old?.extras?.let { l.addAll(it) } } }
    var remarks by remember { mutableStateOf(old?.remarks ?: "") }
    var onlyExtra by remember { mutableStateOf(old?.onlyExtra ?: false) }
    var paid by remember { mutableStateOf(old?.paid ?: false) }
    var showExtra by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val priceL = price.toLongOrNull() ?: 0L
    val portions = if (onlyExtra) 0 else pax
    val extraTotal = extras.sumOf { it.total }
    val total = portions * priceL + extraTotal

    Column(Modifier.fillMaxSize()) {
        SubHeader(if (old == null) "Order baru" else "Edit order", menuBanner(data, dt), onClose)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChipScroll { Chip("Tanggal kirim: " + Fmt.dayShort(dt), true) { pickDate = true } }
            Field("Nama customer", customer, { customer = it; error = "" })
            Field("Lokasi", location, { location = it }, placeholder = "mis. Blok C3 No.5")

            Muted("Jenis order")
            ChipScroll {
                Chip("Porsi", !onlyExtra) { onlyExtra = false }
                Chip("Lauk saja", onlyExtra) { onlyExtra = true }
            }
            if (!onlyExtra) {
                Muted("Jumlah pax")
                Stepper(pax, { pax = it }, min = 1)
                Field("Harga per porsi (Rp)", price, { price = digits(it) }, keyboard = KeyboardType.Number)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Strong("Pesanan tambahan", size = 15)
                Muted("${extras.size} item")
            }
            for ((i, e) in extras.withIndex()) {
                SCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Strong(e.name)
                            Muted("${e.qty} × ${Fmt.rp(e.unitPrice)}" + if (e.note.isNotBlank()) " · ${e.note}" else "")
                        }
                        Strong(Fmt.rp(e.total))
                        TextButton(onClick = { extras.removeAt(i) }) { Text("Hapus", color = c.accentText, fontSize = 12.sp) }
                    }
                }
            }
            OutlineButton("+  Tambah pesanan") { showExtra = true }

            Field("Remarks", remarks, { remarks = it }, placeholder = "mis. tanpa sambal", singleLine = false)

            Muted("Pembayaran")
            ChipScroll {
                Chip("Lunas", paid) { paid = true }
                Chip("Belum bayar", !paid) { paid = false }
            }

            SCard(background = c.surface2) {
                if (!onlyExtra) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Muted("Porsi $pax × ${Fmt.rp(priceL)}", size = 13); Strong(Fmt.rp(pax * priceL))
                    }
                }
                if (extras.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Muted("Pesanan tambahan (${extras.size} item)", size = 13); Strong(Fmt.rp(extraTotal))
                    }
                }
                Rule()
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Strong("Total order", size = 16); Strong(Fmt.rp(total), size = 16, color = c.accentText)
                }
            }
            if (portions > 0) Muted("Kemasan ikut terpotong $portions pcs per jenis kemasan.")
            if (error.isNotEmpty()) Muted(error, color = c.accentText)

            PrimaryButton("Simpan order") {
                if (customer.isBlank()) {
                    error = "Nama customer harus diisi."
                } else if (!onlyExtra && priceL <= 0) {
                    error = "Harga per porsi harus diisi."
                } else if (onlyExtra && extras.isEmpty()) {
                    error = "Lauk saja butuh minimal 1 pesanan tambahan."
                } else {
                    val o = Order(
                        id = old?.id ?: Store.newId(), date = dt.toString(), customer = customer.trim(),
                        location = location.trim(), pax = if (onlyExtra) 0 else pax, remarks = remarks.trim(),
                        portionPrice = priceL, extras = extras.toList(), paid = paid, onlyExtra = onlyExtra, by = old?.by ?: me.idx,
                    )
                    Repo.saveOrder(o, old, me)
                    onClose()
                }
            }
            OutlineButton("Batal", onClick = onClose)
            if (old != null) {
                TextButton(onClick = { confirmDel = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hapus order", color = c.accentText, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (pickDate) DatePickDialog(dt, { dt = it }, { pickDate = false })
    if (showExtra) ExtraDialog(onSave = { extras.add(it); showExtra = false }, onDismiss = { showExtra = false })
    if (confirmDel && old != null) {
        AlertDialog(
            onDismissRequest = { confirmDel = false },
            containerColor = c.surface,
            title = { Text("Hapus order ${old.customer}?", color = c.text, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Order tetap tercatat di Riwayat.", color = c.text2) },
            confirmButton = {
                TextButton(onClick = { Repo.deleteOrder(old, me); confirmDel = false; onClose() }) {
                    Text("Hapus", color = c.accentText, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDel = false }) { Text("Batal", color = c.text2) } },
        )
    }
}

@Composable
fun ExtraDialog(onSave: (Extra) -> Unit, onDismiss: () -> Unit) {
    val c = Sappa.c
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf(1) }
    var price by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val unit = price.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Tambah pesanan", color = c.text, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Muted("Pilih cepat")
                ChipScroll {
                    listOf("Lauk tambahan", "Nasi tambahan", "Sambal").forEach { n -> Chip(n, name == n) { name = n } }
                    Chip("Lainnya", false) { name = "" }
                }
                Field("Tambah apa", name, { name = it })
                Muted("Jumlah")
                Stepper(qty, { qty = it }, min = 1)
                Field("Harga satuan (Rp)", price, { price = digits(it) }, keyboard = KeyboardType.Number)
                Field("Catatan (opsional)", note, { note = it })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Muted("Total tambahan", size = 13); Strong(Fmt.rp(qty * unit))
                }
                Muted("Masuk ke omzet hari itu.")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && unit > 0) onSave(Extra(name.trim(), qty, unit, note.trim()))
            }) { Text("Simpan pesanan", color = c.accentText, fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = c.text2) } },
    )
}
