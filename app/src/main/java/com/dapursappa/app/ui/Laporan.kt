package com.dapursappa.app.ui

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
import java.time.LocalDate

@Composable
fun LaporanTab(
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
        ChipScroll {
            Chip("Harian", tab == 0) { onTab(0) }
            Chip("Mingguan", tab == 1) { onTab(1) }
            Chip("Bulanan", tab == 2) { onTab(2) }
        }
        when (tab) {
            0 -> HarianReport(data, month, onMonth, onDay)
            1 -> MingguanReport(data, week, onWeek, onDay)
            else -> BulananReport(data, month, onMonth)
        }
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
                Muted((d.menuName.ifBlank { "-" }) + " · ${d.pax} pax")
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(Fmt.rp(d.profit), color = if (d.profit >= 0) c.ok else c.accentText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Muted("Omzet ${Fmt.rp(d.revenue)} · Modal ${Fmt.rp(d.cost)}")
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
    Muted("Ketuk hari untuk melihat rincian bahan dan order.")
}

@Composable
private fun MingguanReport(data: AppData, week: LocalDate, onWeek: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit) {
    val days = Calc.week(data, week)
    NavBar(
        "Week ${Fmt.weekNo(week)}", Fmt.range(week, week.plusDays(6)),
        { onWeek(week.minusWeeks(1)) }, { onWeek(week.plusWeeks(1)) },
    )
    StatTile("Profit minggu ini", Fmt.rp(days.sumOf { it.profit }), Modifier.fillMaxWidth(), highlight = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Omzet", Fmt.rp(days.sumOf { it.revenue }), Modifier.weight(1f))
        StatTile("Modal belanja", Fmt.rp(days.sumOf { it.cost }), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Total pax", days.sumOf { it.pax }.toString(), Modifier.weight(1f))
        StatTile("Pengeluaran lain", Fmt.rp(days.sumOf { it.expCost }), Modifier.weight(1f))
    }
    for (d in days) DayRow(d) { onDay(d.date) }
    Muted("Biaya bulanan tidak dihitung di laporan mingguan.")
}

@Composable
private fun BulananReport(data: AppData, month: LocalDate, onMonth: (LocalDate) -> Unit) {
    val c = Sappa.c
    val m = Calc.month(data, month)
    var showUnpaid by remember { mutableStateOf(false) }
    MonthNav(month, onMonth)
    StatTile("Profit bersih bulan ini", Fmt.rp(m.profit), Modifier.fillMaxWidth(), highlight = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Omzet", Fmt.rp(m.revenue), Modifier.weight(1f))
        StatTile("Modal belanja", Fmt.rp(m.modal), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Pengeluaran lain", Fmt.rp(m.expHarian), Modifier.weight(1f))
        StatTile("Biaya bulanan", Fmt.rp(m.monthlyCost), Modifier.weight(1f))
    }

    SectionTitle("Omzet per minggu")
    val groups = m.days.groupBy { Fmt.mondayOf(it.date) }
    SCard {
        var first = true
        for ((monday, list) in groups) {
            if (!first) Rule()
            first = false
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Strong("Week ${Fmt.weekNo(monday)}", size = 13)
                    Muted("${list.first().date.dayOfMonth}–${list.last().date.dayOfMonth} ${Fmt.dayMon(list.first().date).substringAfter(' ')}")
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Strong(Fmt.rp(list.sumOf { it.revenue }), size = 13)
                    Muted("Profit " + Fmt.rp(list.sumOf { it.profit }))
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionTitle("Biaya bulanan"); Strong(Fmt.rp(m.monthlyCost))
    }
    SCard {
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
