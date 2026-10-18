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
import com.dapursappa.app.data.DayCalc
import com.dapursappa.app.data.Expense
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Repo
import com.dapursappa.app.data.Store
import java.time.LocalDate

@Composable
fun SummaryTab(
    me: Me,
    data: AppData,
    weekStart: LocalDate,
    onWeek: (LocalDate) -> Unit,
    onDay: (LocalDate) -> Unit,
    onExpense: () -> Unit,
) {
    val c = Sappa.c
    val days = Calc.week(data, weekStart)
    val pax = days.sumOf { it.pax }
    val omzet = days.sumOf { it.revenue }
    val modal = days.sumOf { it.cost }
    val expH = days.sumOf { it.expCost }
    val profit = days.sumOf { it.profit }
    val end = weekStart.plusDays(6)
    val weekExp = data.expenses.filter { it.date >= weekStart.toString() && it.date <= end.toString() }.sortedBy { it.date }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NavBar(
            "Summary Week ${Fmt.weekNo(weekStart)}", Fmt.range(weekStart, end),
            { onWeek(weekStart.minusWeeks(1)) }, { onWeek(weekStart.plusWeeks(1)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Total pax", pax.toString(), Modifier.weight(1f))
            StatTile("Omzet", Fmt.rp(omzet), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Modal belanja", Fmt.rp(modal), Modifier.weight(1f))
            StatTile("Pengeluaran lain", Fmt.rp(expH), Modifier.weight(1f))
        }
        StatTile("Profit bersih", Fmt.rp(profit), Modifier.fillMaxWidth(), highlight = true)

        for (d in days) DayCard(d, onClick = { onDay(d.date) })

        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionTitle("Pengeluaran lain")
            Strong(Fmt.rp(weekExp.filter { !it.monthly }.sumOf { it.amount }))
        }
        if (weekExp.isEmpty()) Muted("Belum ada pengeluaran lain minggu ini.")
        else SCard {
            for ((i, e) in weekExp.withIndex()) {
                if (i > 0) Rule()
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Strong(e.name)
                        Muted(Fmt.dayName(LocalDate.parse(e.date)) + " · " + userName(data, e.by) + " · " + (if (e.monthly) "Profit Bulanan" else "Profit Harian"))
                    }
                    Strong(Fmt.rp(e.amount))
                }
            }
        }
        PrimaryButton("+  Tambah pengeluaran", onClick = onExpense)
        Spacer(Modifier.height(8.dp))
    }
}

fun userName(data: AppData, idx: Int): String =
    if (idx < 0) "Developer" else data.users.getOrNull(idx)?.name ?: "User ${idx + 1}"

@Composable
fun DayCard(d: DayCalc, onClick: () -> Unit) {
    val c = Sappa.c
    val closed = d.libur && d.orders.isEmpty()
    ClickCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Muted(Fmt.dayName(d.date) + " · " + Fmt.dayMon(d.date), size = 12)
                val menuLines = d.menuName.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (menuLines.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    for (ln in menuLines) Text(ln, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 22.sp)
                } else {
                    Strong(if (closed) "Libur" else "Menu belum diisi", size = 16)
                    if (closed) Muted("Bisa dibuka untuk PO spesial")
                }
            }
            if (closed) Tag("Libur", c.surface2, c.text2)
            else Tag("${d.pax} pax", if (d.pax > 0) c.accentSoft else c.surface2, if (d.pax > 0) c.accentText else c.text2)
        }
        if (d.orders.isNotEmpty() || d.cost > 0) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Muted("Modal " + Fmt.rp(d.cost))
                Text(
                    "Profit " + Fmt.rp(d.profit),
                    color = if (d.profit >= 0) c.ok else c.accentText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
        } else if (!closed) {
            Muted("Belum ada order", modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun DetailHari(me: Me, data: AppData, date: LocalDate, onBack: () -> Unit, onOpenOrder: (String) -> Unit) {
    val c = Sappa.c
    val d = Calc.day(data, date)
    Column(Modifier.fillMaxSize()) {
        SubHeader(Fmt.dayLong(date), (d.menuName.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ").ifBlank { "Menu belum diisi" }) + " · ${d.pax} pax · ${d.orders.size} order", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Omzet", Fmt.rp(d.revenue), Modifier.weight(1f))
                StatTile("Modal", Fmt.rp(d.cost), Modifier.weight(1f))
                StatTile("Profit", Fmt.rp(d.profit), Modifier.weight(1f), highlight = true)
            }
            if (d.expCost > 0) Muted("Profit sudah dikurangi pengeluaran lain ${Fmt.rp(d.expCost)} (${d.expenses.joinToString(", ") { it.name }}).")

            SectionTitle("Bahan dan kemasan")
            SCard {
                Row(Modifier.fillMaxWidth()) {
                    Text("Bahan", Modifier.weight(1.6f), color = c.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Qty", Modifier.weight(1f), color = c.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Harga", Modifier.weight(1f), color = c.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Per porsi", Modifier.weight(1f), color = c.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Rule()
                if (d.bahan.isEmpty() && d.kemasan.isEmpty()) {
                    Muted("Belum ada belanja untuk hari ini.", modifier = Modifier.padding(vertical = 8.dp))
                }
                for (p in d.bahan) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(p.name, Modifier.weight(1.6f), color = c.text, fontSize = 12.sp)
                        Text((Fmt.num(p.qty) + " " + p.unit).trim(), Modifier.weight(1f), color = c.text2, fontSize = 12.sp)
                        Text(Fmt.thousands(p.price), Modifier.weight(1f), color = c.text, fontSize = 12.sp)
                        Text(if (d.pax > 0) Fmt.thousands(Math.round(p.price.toDouble() / d.pax)) else "-", Modifier.weight(1f), color = c.text2, fontSize = 12.sp)
                    }
                }
                for (k in d.kemasan) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(k.name, Modifier.weight(1.6f), color = c.text, fontSize = 12.sp)
                        Text("${k.pcs} pcs", Modifier.weight(1f), color = c.text2, fontSize = 12.sp)
                        Text(Fmt.thousands(k.cost), Modifier.weight(1f), color = c.text, fontSize = 12.sp)
                        Text(Fmt.thousands(Math.round(k.unitPrice)), Modifier.weight(1f), color = c.text2, fontSize = 12.sp)
                    }
                }
                Rule()
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("Total", Modifier.weight(1.6f), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text("", Modifier.weight(1f))
                    Text(Fmt.thousands(d.cost), Modifier.weight(1f), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (d.pax > 0) Fmt.thousands(Math.round(d.costPerPax)) else "-", Modifier.weight(1f), color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (d.pax > 0) Muted("Per porsi = harga ÷ ${d.pax} pax terjual. Biaya bulanan tidak ikut dihitung di sini.")

            SectionTitle("Order hari ini")
            if (d.orders.isEmpty()) Muted("Belum ada order.")
            for (o in d.orders) {
                ClickCard(onClick = { if (me.canAdmin) onOpenOrder(o.id) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Strong(o.customer)
                            val extra = if (o.extrasTotal > 0) " · + tambahan ${Fmt.rp(o.extrasTotal)}" else ""
                            Muted((if (o.location.isBlank()) "" else o.location + " · ") + (if (o.onlyExtra) "lauk saja" else "${o.pax} pax") + extra)
                        }
                        Strong(Fmt.rp(o.total))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ExpenseScreen(me: Me, data: AppData, date: LocalDate, onBack: () -> Unit) {
    val c = Sappa.c
    var kind by remember { mutableStateOf("Bensin") }
    var desc by remember { mutableStateOf("Bensin motor antar") }
    var amount by remember { mutableStateOf("") }
    var dt by remember { mutableStateOf(date) }
    var monthly by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val monday = Fmt.mondayOf(dt)
    val list = data.expenses.filter { it.date >= monday.toString() && it.date <= monday.plusDays(6).toString() }.sortedBy { it.date }

    Column(Modifier.fillMaxSize()) {
        SubHeader("Pengeluaran lain", Fmt.dayLong(dt), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Muted("Jenis")
            ChipFlow {
                listOf("Bensin", "Tambal ban", "Parkir", "Servis motor", "Lainnya").forEach { k ->
                    Chip(k, kind == k) {
                        kind = k
                        desc = when (k) { "Bensin" -> "Bensin motor antar"; "Lainnya" -> ""; else -> k }
                    }
                }
            }
            Field("Keterangan", desc, { desc = it; error = "" })
            Field("Nominal (Rp)", amount, { amount = digits(it); error = "" }, keyboard = KeyboardType.Number)
            ChipScroll { Chip("Tanggal: " + Fmt.dayShort(dt), true) { pick = true } }
            Muted("Charge ke")
            ChipScroll {
                Chip("Profit Harian", !monthly) { monthly = false }
                Chip("Profit Bulanan", monthly) { monthly = true }
            }
            Muted("Profit Harian: mengurangi profit hari itu, lalu ikut ke mingguan dan bulanan. Profit Bulanan: hanya mengurangi profit bulanan.")
            if (error.isNotEmpty()) Muted(error, color = c.accentText)
            PrimaryButton("Simpan") {
                val a = amount.toLongOrNull() ?: 0L
                if (desc.isBlank()) error = "Keterangan harus diisi."
                else if (a <= 0) error = "Nominal harus diisi."
                else {
                    Repo.saveExpense(Expense(Store.newId(), dt.toString(), desc.trim(), a, monthly, me.idx), me)
                    amount = ""
                    error = ""
                }
            }

            SectionTitle("Pengeluaran minggu ini")
            if (list.isEmpty()) Muted("Belum ada.")
            for (e in list) {
                SCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Strong(e.name)
                            Muted(Fmt.dayName(LocalDate.parse(e.date)) + " · " + userName(data, e.by))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Strong(Fmt.rp(e.amount))
                            if (e.monthly) Tag("Bulanan", c.warnSoft, c.warn) else Tag("Harian", c.okSoft, c.ok)
                        }
                        if (canEdit(me, e.date)) {
                            TextButton(onClick = { Repo.deleteExpense(e, me) }) { Text("Hapus", color = c.accentText, fontSize = 12.sp) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (pick) DatePickDialog(dt, { dt = it }, { pick = false })
}
