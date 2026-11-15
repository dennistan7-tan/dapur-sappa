package com.dapursappa.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dapursappa.app.data.AppData
import com.dapursappa.app.data.Calc
import com.dapursappa.app.data.Fmt
import java.time.LocalDate

@Composable
fun AnalisisScreen(data: AppData, onBack: () -> Unit) {
    val a = Calc.menuAnalysis(data, LocalDate.now())
    var sort by remember { mutableIntStateOf(0) }
    val items = when (sort) {
        1 -> a.items.sortedWith(compareByDescending<com.dapursappa.app.data.MenuStat> { it.count }.thenByDescending { it.mpp })
        2 -> a.items.sortedWith(compareByDescending<com.dapursappa.app.data.MenuStat> { it.pax }.thenByDescending { it.mpp })
        else -> a.items.sortedWith(compareByDescending<com.dapursappa.app.data.MenuStat> { it.mpp }.thenByDescending { it.count })
    }
    Column(Modifier.fillMaxWidth()) {
        SubHeader("Analisis menu", "Hanya developer", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Margin rata-rata per pax (overall)", Fmt.rp(a.mpp), Modifier.fillMaxWidth(), highlight = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Hari terhitung", a.days.toString(), Modifier.weight(1f))
                StatTile("Total pax", a.pax.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Omzet", Fmt.rp(a.revenue), Modifier.weight(1f))
                StatTile("Profit harian", Fmt.rp(a.profit), Modifier.weight(1f))
            }
            SectionTitle("Ranking menu")
            ChipScroll {
                Chip("Margin/pax", sort == 0) { sort = 0 }
                Chip("Paling sering", sort == 1) { sort = 1 }
                Chip("Pax terbanyak", sort == 2) { sort = 2 }
            }
            if (items.isEmpty()) Muted("Belum ada hari dengan order dan modal terinput.")
            SCard {
                for ((i, m) in items.withIndex()) {
                    if (i > 0) Rule()
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Strong("${i + 1}. ${m.name}", size = 14)
                            Muted("Dibuat ${m.count}× · ${m.pax} pax")
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Strong(Fmt.rp(m.mpp) + "/pax", size = 14)
                            Muted(if (a.mpp > 0) "vs rata-rata " + (if (m.mpp >= a.mpp) "+" else "-") + Fmt.rp(Math.abs(m.mpp - a.mpp)) else "")
                        }
                    }
                }
            }
            SectionTitle("Daftar bahan belanja (A-Z)")
            val buy = Calc.buyList(data)
            if (buy.isEmpty()) Muted("Belum ada belanja.")
            SCard {
                for ((i, b) in buy.withIndex()) {
                    if (i > 0) Rule()
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Strong(b.name, size = 14)
                            Muted("${b.times}× beli · " + b.units.joinToString(" + ") { Fmt.num(it.qty) + " " + it.unit })
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Strong(Fmt.rp(b.spend), size = 14)
                            Muted(b.units.filter { it.unit.isNotBlank() && it.qty > 0 }.joinToString(" · ") { Fmt.rp(it.unitPrice) + "/" + it.unit })
                        }
                    }
                }
            }
            Muted("Margin per pax = profit harian ÷ pax di hari menu itu dibuat (sudah dikurangi bahan, kemasan, pengeluaran harian). Satu hari punya beberapa menu, jadi margin dibagi rata ke semua menu hari itu. Nasi putih tidak dihitung.")
        }
    }
}
