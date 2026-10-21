package com.dapursappa.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapursappa.app.data.AppData
import com.dapursappa.app.data.Calc
import com.dapursappa.app.data.DayCalc
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Order
import com.dapursappa.app.data.Repo
import com.dapursappa.app.data.CustCalc
import com.dapursappa.app.data.CustWeek
import java.time.LocalDate

@Composable
fun LaporanTab(
    me: Me,
    data: AppData,
    tab: Int,
    onTab: (Int) -> Unit,
    month: LocalDate,
    onMonth: (LocalDate) -> Unit,
    week: LocalDate,
    onWeek: (LocalDate) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        var xmsg by remember { mutableStateOf("") }
        ChipScroll {
            Chip("Harian", tab == 0) { onTab(0) }
            Chip("Mingguan", tab == 1) { onTab(1) }
            Chip("Bulanan", tab == 2) { onTab(2) }
            Chip("Customer", tab == 3) { onTab(3) }
        }
        when (tab) {
            0 -> HarianReport(data, month, onMonth, onDay)
            1 -> MingguanReport(data, week, onWeek, onDay)
            3 -> CustomerReport(me, data, month, onMonth)
            else -> BulananReport(data, month, onMonth, onDay)
        }
        OutlineButton("Export laporan ke Excel (bulan, minggu, hari, pengeluaran)") { xmsg = exportLaporanExcel(ctx, data) }
        if (xmsg.isNotEmpty()) Muted(xmsg)
        Spacer(Modifier.height(8.dp))
    }
}

private fun monthSub(month: LocalDate): String {
    val today = LocalDate.now()
    val last = month.lengthOfMonth()
    return if (month.year == today.year && month.month == today.month) {
        "1 – ${today.dayOfMonth} ${Fmt.dayMon(month).substringAfter(' ')} (berjalan)"
    } else "1 – $last ${Fmt.dayMon(month).substringAfter(' ')}"
}

@Composable
private fun MonthNav(month: LocalDate, onMonth: (LocalDate) -> Unit) {
    NavBar(Fmt.monthName(month), monthSub(month), { onMonth(month.minusMonths(1)) }, { onMonth(month.plusMonths(1)) })
}

@Composable
private fun DayRow(d: DayCalc, onClick: () -> Unit) {
    val c = Sappa.c
    ClickCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Strong(Fmt.dayShort(d.date), size = 14)
                Muted((d.menuName.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ").ifBlank { "-" }) + " · ${d.pax} pax")
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(Fmt.rp(d.profit), color = if (d.profit >= 0) c.ok else c.accentText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Muted("Omzet ${Fmt.rp(d.revenue)} · Modal ${Fmt.rp(d.cost)}")
                Muted("Margin ${Fmt.pct(d.profit, d.revenue)}")
            }
        }
    }
}

@Composable
private fun HarianReport(data: AppData, month: LocalDate, onMonth: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit) {
    val m = Calc.month(data, month)
    MonthNav(month, onMonth)
    val active = m.days.filter { it.revenue > 0 || it.cost > 0 || it.expCost > 0 }.reversed()
    if (active.isEmpty()) Muted("Belum ada data di bulan ini.")
    for (d in active) DayRow(d) { onDay(d.date) }
    Muted("Ketuk hari untuk melihat rincian bahan dan order. Margin = profit ÷ omzet.")
}

@Composable
private fun MingguanReport(data: AppData, week: LocalDate, onWeek: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit) {
    val days = Calc.week(data, week)
    NavBar(
        "Week ${Fmt.weekNo(week)}", Fmt.range(week, week.plusDays(6)),
        { onWeek(week.minusWeeks(1)) }, { onWeek(week.plusWeeks(1)) },
    )
    val rev = days.sumOf { it.revenue }
    val pr = days.sumOf { it.profit }
    StatTile("Gross Profit minggu ini", Fmt.rp(pr), Modifier.fillMaxWidth(), highlight = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Margin", Fmt.pct(pr, rev), Modifier.weight(1f))
        StatTile("Omzet", Fmt.rp(rev), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Modal belanja", Fmt.rp(days.sumOf { it.cost }), Modifier.weight(1f))
        StatTile("Total pax", days.sumOf { it.pax }.toString(), Modifier.weight(1f))
    }
    for (d in days) DayRow(d) { onDay(d.date) }
    Muted("Margin mingguan = gross profit ÷ omzet. Biaya bulanan tidak dihitung di laporan mingguan.")
}

@Composable
private fun BulananReport(data: AppData, month: LocalDate, onMonth: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit) {
    val c = Sappa.c
    val m = Calc.month(data, month)
    var showUnpaid by remember { mutableStateOf(false) }
    val openWeeks = remember { mutableStateListOf<String>() }
    MonthNav(month, onMonth)
    StatTile("Profit bersih bulan ini", Fmt.rp(m.profit), Modifier.fillMaxWidth(), highlight = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Margin bersih", Fmt.pct(m.profit, m.revenue), Modifier.weight(1f))
        StatTile("Margin sebelum biaya bulanan", Fmt.pct(m.profit + m.monthlyCost, m.revenue), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Omzet", Fmt.rp(m.revenue), Modifier.weight(1f))
        StatTile("Modal belanja", Fmt.rp(m.modal), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Pengeluaran lain", Fmt.rp(m.expHarian), Modifier.weight(1f))
        StatTile("Biaya bulanan", Fmt.rp(m.monthlyCost), Modifier.weight(1f))
    }

    SectionTitle("Omzet dan margin per minggu")
    val groups = m.days.groupBy { Fmt.mondayOf(it.date) }
    SCard {
        var first = true
        for ((monday, list) in groups) {
            if (!first) Rule()
            first = false
            val key = monday.toString()
            val open = openWeeks.contains(key)
            val rev = list.sumOf { it.revenue }
            val pr = list.sumOf { it.profit }
            Row(
                Modifier.fillMaxWidth().clickable { if (open) openWeeks.remove(key) else openWeeks.add(key) }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Strong((if (open) "▾ " else "▸ ") + "Week ${Fmt.weekNo(monday)}", size = 13)
                    Muted("${list.first().date.dayOfMonth}–${list.last().date.dayOfMonth} ${Fmt.dayMon(list.first().date).substringAfter(' ')} · ketuk untuk data harian")
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Strong(Fmt.rp(rev), size = 13)
                    Muted("Profit " + Fmt.rp(pr) + " · Margin " + Fmt.pct(pr, rev))
                }
            }
            if (open) {
                val act = list.filter { it.revenue > 0 || it.cost > 0 || it.expCost > 0 }
                if (act.isEmpty()) Muted("Belum ada data harian di minggu ini.", modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                for (d in act) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp).clickable { onDay(d.date) }.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Strong(Fmt.dayShort(d.date), size = 13)
                            Muted((d.menuName.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ").ifBlank { "-" }) + " · ${d.pax} pax")
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Strong(Fmt.rp(d.revenue), size = 13)
                            Muted("Profit " + Fmt.rp(d.profit) + " · Margin " + Fmt.pct(d.profit, d.revenue))
                        }
                    }
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionTitle("Biaya bulanan"); Strong(Fmt.rp(m.monthlyCost))
    }
    SCard(tone = Tone.Shop) {
        if (m.monthlyLines.isEmpty()) Muted("Belum ada biaya bulanan.")
        for ((i, line) in m.monthlyLines.withIndex()) {
            if (i > 0) Rule()
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(line.first, color = c.text, fontSize = 14.sp)
                Strong(Fmt.thousands(line.second), size = 14)
            }
        }
    }
    Muted("Biaya bulanan hanya dihitung di laporan bulanan.")

    ClickCard(onClick = { showUnpaid = !showUnpaid }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Strong("Belum dibayar customer", color = c.warn)
            Strong(Fmt.rp(m.unpaidOrders.sumOf { it.total }), color = c.warn)
        }
        Muted("${m.unpaidOrders.size} order · ketuk untuk " + if (showUnpaid) "menutup" else "melihat rincian")
        if (showUnpaid) {
            Spacer(Modifier.height(8.dp))
            for (o in m.unpaidOrders.sortedBy { it.date }) {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(o.customer + " · " + Fmt.dayShort(LocalDate.parse(o.date)), color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(Fmt.rp(o.total), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
private fun CustomerReport(me: Me, data: AppData, month: LocalDate, onMonth: (LocalDate) -> Unit) {
    val c = Sappa.c
    val r = Calc.customers(data, month)
    val open = remember { mutableStateListOf<String>() }
    var payFor by remember { mutableStateOf<Pair<CustCalc, CustWeek>?>(null) }
    MonthNav(month, onMonth)
    StatTile("Total customer bulan ini", r.customers.toString(), Modifier.fillMaxWidth(), highlight = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Total order", r.orders.toString(), Modifier.weight(1f))
        StatTile("Total pax", r.pax.toString(), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Omzet", Fmt.rp(r.revenue), Modifier.weight(1f))
        StatTile("Profit gain", Fmt.rp(r.profit), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Profit rate", Fmt.pct(r.profit, r.revenue), Modifier.weight(1f))
        StatTile("Belum lunas", Fmt.rp(r.unpaid), Modifier.weight(1f))
    }
    SectionTitle("Per customer")
    if (r.list.isEmpty()) Muted("Belum ada order di bulan ini.")
    for (cu in r.list) {
        val isOpen = open.contains(cu.key)
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(if (cu.allPaid) c.okSoft else c.surface)
                .border(if (cu.allPaid) 2.dp else 1.dp, if (cu.allPaid) c.ok else c.border, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { if (isOpen) open.remove(cu.key) else open.add(cu.key) },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Strong((if (isOpen) "▾ " else "▸ ") + cu.name, size = 15)
                    Muted("${cu.orders.size} order · ${cu.pax} pax")
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Strong(Fmt.rp(cu.revenue), size = 15)
                    Muted("Profit ${Fmt.rp(cu.profit)} · Rate ${Fmt.pct(cu.profit, cu.revenue)}")
                }
            }
            Spacer(Modifier.height(4.dp))
            if (cu.allPaid) Tag("Lunas semua", c.okSoft, c.ok) else Muted("Belum lunas ${Fmt.rp(cu.unpaidSum)}")
            if (isOpen) {
                for (w in cu.weeks) {
                    Spacer(Modifier.height(8.dp))
                    Rule()
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .background(if (w.paid) c.okSoft else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(if (w.paid) 8.dp else 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Strong("Week ${Fmt.weekNo(w.week)} · ${w.orders.size} order · ${w.pax} pax", size = 13)
                            Muted(w.orders.joinToString(", ") { Fmt.dayShort(LocalDate.parse(it.date)).substringBefore(',') + " " + LocalDate.parse(it.date).dayOfMonth })
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Strong(Fmt.rp(w.revenue), size = 13)
                            if (w.paid) Tag("Lunas" + (if (w.methods.isNotEmpty()) " · " + (if (w.methods.size > 1) "Campur" else w.methods[0]) else ""), c.okSoft, c.ok)
                            else Muted("Belum lunas")
                        }
                    }
                    if (w.paid) {
                        TextButton(onClick = {
                            Repo.markPaid(w.orders.filter { it.total > 0 && it.paid }, false, "", me)
                        }) { Text("Batalkan lunas", color = c.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        PrimaryButton("Tandai lunas ${Fmt.rp(w.unpaidSum)}") { payFor = cu to w }
                    }
                }
            }
        }
    }
    Muted("Profit per customer = omzet dikurangi modal bahan dan kemasan per porsi di hari order. Pengeluaran lain dan biaya bulanan tidak dibagi ke customer. Rate = profit ÷ omzet.")

    payFor?.let { (cu, w) ->
        var method by remember(cu.key, w.week) { mutableStateOf("") }
        var custom by remember(cu.key, w.week) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { payFor = null },
            title = { Text("Tandai lunas", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${cu.name} · Week ${Fmt.weekNo(w.week)} · ${Fmt.rp(w.unpaidSum)}", color = c.text, fontSize = 14.sp)
                    Muted("Dibayar via")
                    ChipScroll {
                        for (m in listOf("Cash", "Transfer", "Lainnya")) Chip(m, method == m) { method = m }
                    }
                    if (method == "Lainnya") Field("Metode lain", custom, { custom = it }, placeholder = "mis. QRIS, GoPay")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = method.isNotEmpty(),
                    onClick = {
                        val mm = if (method == "Lainnya") custom.trim().ifBlank { "Lainnya" } else method
                        Repo.markPaid(w.unpaid, true, mm, me)
                        payFor = null
                    },
                ) { Text("Simpan lunas", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { payFor = null }) { Text("Batal") } },
        )
    }
}
