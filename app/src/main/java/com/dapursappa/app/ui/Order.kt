package com.dapursappa.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    val orders = data.orders.filter { it.date == ds }.sortedWith(compareBy({ it.sortKey }, { it.customer.lowercase() }))
    val pax = orders.sumOf { it.portions }
    val omzet = orders.sumOf { it.total }
    val unpaid = orders.filter { !it.paid && it.total > 0 }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrimaryButton("+  Tambah order") { onOpen(null, date) }
        MenuBox(data, date, if (date == LocalDate.now()) "MENU HARI INI" else "MENU")
        NavBar("Order", "", { onDate(date.minusDays(1)) }, { onDate(date.plusDays(1)) })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Order", orders.size.toString(), Modifier.weight(1f), tone = Tone.Order)
            StatTile("Porsi", pax.toString(), Modifier.weight(1f), tone = Tone.Order)
            StatTile("Omzet", Fmt.rp(omzet), Modifier.weight(1.4f), highlight = true)
        }

        SectionTitle("Daftar order")
        if (orders.isEmpty()) Muted("Belum ada order di tanggal ini.", modifier = Modifier.padding(vertical = 8.dp))
        for (o in orders) {
            ClickCard(onClick = { onOpen(o.id, date) }, tone = Tone.Order) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (o.time.isNotBlank()) {
                            Text(o.time, color = c.accentText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(8.dp))
                        }
                        Strong(o.customer, size = 19, modifier = Modifier.weight(1f, fill = false))
                    }
                    Spacer(Modifier.width(8.dp))
                    Strong(Fmt.rp(o.total), size = 19)
                }
                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (o.location.isNotBlank()) {
                        Text(o.location, color = c.text2, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
                        Text("  ·  ", color = c.text2, fontSize = 13.sp)
                    }
                    Text(if (o.onlyExtra) "tanpa porsi" else "${o.pax} pax", color = c.accentText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (o.remarks.isNotBlank()) NoteHighlight("Catatan: " + o.remarks, Modifier.padding(top = 6.dp))
                for (e in o.extras) {
                    NoteHighlight("Tambahan: ${e.name} ${e.qty} × ${Fmt.rp(e.unitPrice)}", Modifier.padding(top = 6.dp))
                }
                val hasTags = o.delivery.isNotBlank() || o.onlyExtra || o.extrasTotal > 0 || o.shipping > 0
                if (hasTags) {
                    Spacer(Modifier.height(8.dp))
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (o.delivery.isNotBlank()) Tag(o.delivery, c.accentSoft, c.accentText)
                        if (o.onlyExtra) Tag("Lauk saja", c.accentSoft, c.accentText)
                        else if (o.extrasTotal > 0) Tag("+ Tambahan ${Fmt.rp(o.extrasTotal)}", c.accentSoft, c.accentText)
                        if (o.shipping > 0) Tag("Ongkir ${Fmt.rp(o.shipping)}", c.accentSoft, c.accentText)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private val SLOTS = listOf("PAGI" to "07:00", "SIANG" to "12:00", "SORE" to "16:00", "MALAM" to "19:00")

@Composable
fun OrderForm(me: Me, data: AppData, orderId: String?, date: LocalDate, onClose: () -> Unit) {
    val c = Sappa.c
    val old = remember(orderId) { data.orders.firstOrNull { it.id == orderId } }
    var dt by remember { mutableStateOf(old?.let { LocalDate.parse(it.date) } ?: date) }
    val days = remember { mutableStateListOf<LocalDate>().also { it.add(old?.let { o -> LocalDate.parse(o.date) } ?: date) } }
    var customer by remember { mutableStateOf(old?.customer ?: "") }
    var location by remember { mutableStateOf(old?.location ?: "") }
    var pax by remember { mutableStateOf(old?.pax ?: 1) }
    var price by remember { mutableStateOf((old?.portionPrice ?: Calc.defaultPrice(data, date)).toString()) }
    var ship by remember { mutableStateOf(if ((old?.shipping ?: 0L) > 0L) old!!.shipping.toString() else "") }
    val extras = remember { mutableStateListOf<Extra>().also { l -> old?.extras?.let { l.addAll(it) } } }
    var remarks by remember { mutableStateOf(old?.remarks ?: "") }
    var onlyExtra by remember { mutableStateOf(old?.onlyExtra ?: false) }
    var paid by remember { mutableStateOf(old?.paid ?: false) }
    var slot by remember { mutableStateOf(old?.slot ?: "") }
    var hh by remember { mutableStateOf(old?.time?.substringBefore(':', "") ?: "") }
    var mm by remember { mutableStateOf(old?.time?.substringAfter(':', "") ?: "") }
    var showExtra by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val priceL = price.toLongOrNull() ?: 0L
    val portions = if (onlyExtra) 0 else pax
    val extraTotal = extras.sumOf { it.total }
    val shipL = ship.toLongOrNull() ?: 0L
    val total = portions * priceL + extraTotal + shipL
    val nd = if (old != null) 1 else maxOf(1, days.size)

    Column(Modifier.fillMaxSize()) {
        SubHeader(if (old == null) "Order baru" else "Edit order", menuBanner(data, dt), onClose)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (old != null) {
                ChipScroll { Chip("Tanggal kirim: " + Fmt.dayShort(dt), true) { pickDate = true } }
            } else {
                val mon = Fmt.mondayOf(dt)
                ChipScroll { Chip("Pilih minggu: " + Fmt.dayShort(dt), true) { pickDate = true } }
                Muted("Hari kirim (boleh lebih dari satu)")
                ChipFlow {
                    for (i in 0 until 7) {
                        val d = mon.plusDays(i.toLong())
                        Chip(Fmt.dayName(d).take(3) + " " + d.dayOfMonth, days.contains(d)) {
                            if (days.contains(d)) days.remove(d) else days.add(d)
                            days.sort()
                            if (days.isNotEmpty()) dt = days.first()
                        }
                    }
                    Chip("Sen–Jum", false) { days.clear(); for (i in 0 until 5) days.add(mon.plusDays(i.toLong())); dt = days.first() }
                    Chip("Sen–Sab", false) { days.clear(); for (i in 0 until 6) days.add(mon.plusDays(i.toLong())); dt = days.first() }
                }
                Muted("${days.size} hari dipilih. Satu nama customer, order yang sama dibuat untuk tiap hari.")
            }
            Field("Nama customer", customer, { customer = it; error = "" })
            Field("Lokasi", location, { location = it }, placeholder = "mis. Blok C3 No.5")

            Muted("Waktu pengantaran (info untuk driver)")
            ChipScroll {
                SLOTS.forEach { (name, def) ->
                    Chip(name, slot == name) {
                        if (slot == name) { slot = ""; hh = ""; mm = "" }
                        else {
                            slot = name
                            hh = def.substringBefore(':'); mm = def.substringAfter(':')
                        }
                        error = ""
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                Field("Jam (0-23)", hh, { hh = it.filter { ch -> ch.isDigit() }.take(2); error = "" }, Modifier.weight(1f), keyboard = KeyboardType.Number, placeholder = "16")
                Text(":", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 14.dp))
                Field("Menit (0-59)", mm, { mm = it.filter { ch -> ch.isDigit() }.take(2); error = "" }, Modifier.weight(1f), keyboard = KeyboardType.Number, placeholder = "00")
            }

            Muted("Jenis order")
            ChipScroll {
                Chip("Porsi", !onlyExtra) { onlyExtra = false }
                Chip("Lauk saja", onlyExtra) { onlyExtra = true }
            }
            if (!onlyExtra) {
                Muted("Jumlah pax")
                Stepper(pax, { pax = it }, min = 1)
                Field("Harga per porsi (Rp)", price, { price = digits(it) }, keyboard = KeyboardType.Number)
                Field("Charge ongkir (Rp)", ship, { ship = digits(it) }, keyboard = KeyboardType.Number, placeholder = "0")
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

            SCard(tone = Tone.Money) {
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
                if (shipL > 0) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Muted("Charge ongkir", size = 13); Strong(Fmt.rp(shipL))
                    }
                }
                Rule()
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Strong("Total per hari", size = 16); Strong(Fmt.rp(total), size = 16, color = c.accentText)
                }
                if (nd > 1) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Strong("Total $nd hari", size = 16); Strong(Fmt.rp(total * nd), size = 16, color = c.accentText)
                    }
                }
            }
            if (portions > 0) Muted("Kemasan ikut terpotong $portions pcs per jenis kemasan.")
            if (error.isNotEmpty()) Muted(error, color = c.accentText)

            PrimaryButton(if (nd > 1) "Simpan $nd order" else "Simpan order") {
                val hv = hh.toIntOrNull(); val mv = if (mm.isBlank()) 0 else mm.toIntOrNull()
                val tn: String? = if (hh.isBlank() && mm.isBlank()) "" else if (hv == null || mv == null || hv !in 0..23 || mv !in 0..59) null else "%02d:%02d".format(hv, mv)
                if (tn != null && tn.isNotEmpty() && slot.isEmpty()) slot = when { hv!! < 11 -> "PAGI"; hv < 15 -> "SIANG"; hv < 18 -> "SORE"; else -> "MALAM" }
                if (customer.isBlank()) {
                    error = "Nama customer harus diisi."
                } else if (!onlyExtra && priceL <= 0) {
                    error = "Harga per porsi harus diisi."
                } else if (onlyExtra && extras.isEmpty()) {
                    error = "Lauk saja butuh minimal 1 pesanan tambahan."
                } else if (old == null && days.isEmpty()) {
                    error = "Pilih minimal 1 hari kirim."
                } else if (tn == null) {
                    error = "Jam atau menit tidak valid. Contoh: jam 16, menit 00"
                } else {
                    fun mk(id: String, d: LocalDate) = Order(
                        id = id, date = d.toString(), customer = customer.trim(),
                        location = location.trim(), pax = if (onlyExtra) 0 else pax, remarks = remarks.trim(),
                        portionPrice = priceL, extras = extras.toList(), paid = paid, onlyExtra = onlyExtra, by = old?.by ?: me.idx,
                        slot = slot, time = tn, payMethod = if (paid) old?.payMethod.orEmpty() else "", shipping = shipL,
                    )
                    if (old != null) Repo.saveOrder(mk(old.id, dt), old, me)
                    else Repo.saveOrders(days.sorted().map { mk(Store.newId(), it) }, me)
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

    if (pickDate) DatePickDialog(dt, { dt = it; if (old == null) { days.clear(); days.add(it) } }, { pickDate = false })
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
