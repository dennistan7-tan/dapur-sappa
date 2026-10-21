package com.dapursappa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
    data: AppData,
    weekStart: LocalDate,
    onWeek: (LocalDate) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val c = Sappa.c
    val days = Calc.week(data, weekStart)
    val pax = days.sumOf { it.pax }
    val omzet = days.sumOf { it.revenue }
    val profit = days.sumOf { it.profit }
    val end = weekStart.plusDays(6)

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
        StatTile("Gross Profit", Fmt.rp(profit), Modifier.fillMaxWidth(), highlight = true)

        SectionTitle("Menu minggu ini")
        for (d in days) DayCard(d, onClick = { onDay(d.date) })
        Spacer(Modifier.height(8.dp))
    }
}

fun userName(data: AppData, idx: Int): String =
    if (idx < 0) "Developer" else data.users.getOrNull(idx)?.name ?: "User"

@Composable
fun DayCard(d: DayCalc, onClick: () -> Unit) {
    val c = Sappa.c
    val closed = d.libur && d.orders.isEmpty()
    ClickCard(onClick = onClick, tone = Tone.Menu, outline = d.date == java.time.LocalDate.now()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.dayName(d.date) + " · " + Fmt.dayMon(d.date), color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
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
            SCard(tone = Tone.Shop) {
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
                ClickCard(onClick = { onOpenOrder(o.id) }, tone = Tone.Order) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Strong(o.customer)
                            val extra = if (o.extrasTotal > 0) " · + tambahan ${Fmt.rp(o.extrasTotal)}" else ""
                            Muted((if (o.delivery.isBlank()) "" else o.delivery + " · ") + (if (o.location.isBlank()) "" else o.location + " · ") + (if (o.onlyExtra) "lauk saja" else "${o.pax} pax") + extra)
                        }
                        Strong(Fmt.rp(o.total))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
