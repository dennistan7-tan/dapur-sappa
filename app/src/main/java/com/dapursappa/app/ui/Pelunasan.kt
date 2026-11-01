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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapursappa.app.data.AppData
import com.dapursappa.app.data.Calc
import com.dapursappa.app.data.ConfCust
import com.dapursappa.app.data.Fmt
import com.dapursappa.app.data.Income
import com.dapursappa.app.data.Me
import com.dapursappa.app.data.Repo
import java.time.LocalDate

private class IncRow(reason: String, custom: String, amount: String) {
    var reason by mutableStateOf(reason)
    var custom by mutableStateOf(custom)
    var amount by mutableStateOf(amount)
}

@Composable
fun PelunasanScreen(me: Me, data: AppData, month: LocalDate, onMonth: (LocalDate) -> Unit, onBack: () -> Unit) {
    val c = Sappa.c
    val weeks = Calc.confirmWeeks(data, month)
    var dlg by remember { mutableStateOf<ConfCust?>(null) }
    var cancel by remember { mutableStateOf<ConfCust?>(null) }
    Column(Modifier.fillMaxWidth()) {
        SubHeader("Konfirmasi Pembayaran", "Pelunasan customer per minggu", onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthNav(month, onMonth)
            val mon = Fmt.monthName(month).substringBefore(' ').take(3)
            for ((i, w) in weeks.withIndex()) {
                val end = w.week.plusDays(6)
                SectionTitle("$mon - Week ${i + 1} (tgl ${w.week.dayOfMonth} sampai tgl ${end.dayOfMonth} ${Fmt.dayMon(end).substringAfter(' ')})")
                if (w.rows.isEmpty()) Muted("Tidak ada order di minggu ini.")
                else SCard {
                    for ((j, r) in w.rows.withIndex()) {
                        if (j > 0) Rule()
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Strong("${j + 1}. ${r.name}", size = 14)
                                Muted("Tagihan ${Fmt.rp(r.bill)}")
                                if (r.paid) {
                                    val s = r.settlement
                                    val pm = s?.method?.ifBlank { null } ?: r.orders.map { it.payMethod }.filter { it.isNotBlank() }.distinct().joinToString("/")
                                    Spacer(Modifier.height(2.dp))
                                    Tag("Lunas" + if (pm.isNotBlank()) " · $pm" else "", c.okSoft, c.ok)
                                    if (s != null && s.extra > 0) Muted("+ ${Fmt.rp(s.extra)} " + s.incomes.joinToString(", ", "(", ")") { it.reason })
                                    if (s != null && s.note.isNotBlank()) Muted("Catatan: ${s.note}")
                                } else if (r.unpaid != r.bill) {
                                    Muted("Sisa belum lunas ${Fmt.rp(r.unpaid)}")
                                }
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                if (r.paid) {
                                    TextButton(onClick = { dlg = r }) { Text("Ubah", color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                    TextButton(onClick = { cancel = r }) { Text("Batalkan lunas", color = c.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                } else {
                                    androidx.compose.material3.Button(
                                        onClick = { dlg = r },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                                    ) { Text("Submit pelunasan", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                        }
                    }
                    Rule()
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Muted("Belum lunas minggu ini")
                        Strong(Fmt.rp(w.unpaid), size = 13)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    dlg?.let { r -> PayDialog(me, r) { dlg = null } }
    cancel?.let { r ->
        AlertDialog(
            onDismissRequest = { cancel = null },
            title = { Text("Batalkan lunas?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("${r.name} · ${Fmt.rp(r.bill)}. Order kembali Belum bayar dan pemasukan tambahan ikut dihapus.", fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { Repo.cancelSettlement(r, me); cancel = null }) { Text("Ya, batalkan", fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { cancel = null }) { Text("Kembali") } },
        )
    }
}

@Composable
private fun PayDialog(me: Me, r: ConfCust, onClose: () -> Unit) {
    val c = Sappa.c
    val s = r.settlement
    val known = listOf("Cash", "Transfer")
    val oldPm = s?.method?.ifBlank { null } ?: r.orders.map { it.payMethod }.firstOrNull { it.isNotBlank() } ?: ""
    var method by remember { mutableStateOf(if (oldPm in known) oldPm else if (oldPm.isNotBlank()) "Lainnya" else "") }
    var custom by remember { mutableStateOf(if (oldPm.isNotBlank() && oldPm !in known) oldPm else "") }
    var note by remember { mutableStateOf(s?.note ?: "") }
    val rows = remember {
        mutableStateListOf<IncRow>().also { l ->
            s?.incomes?.forEach { l.add(IncRow(if (it.reason in listOf("Ongkir", "Tip")) it.reason else "Lainnya", if (it.reason in listOf("Ongkir", "Tip")) "" else it.reason, it.amount.toString())) }
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (s != null || r.paid) "Ubah pelunasan" else "Submit pelunasan", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${r.name} · Tagihan ${Fmt.rp(r.bill)}", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Muted("Dibayar via")
                ChipScroll { for (m in listOf("Cash", "Transfer", "Lainnya")) Chip(m, method == m) { method = m } }
                if (method == "Lainnya") Field("Metode lain", custom, { custom = it }, placeholder = "mis. QRIS, GoPay")
                Muted("Pemasukan tambahan (opsional)")
                for ((i, row) in rows.withIndex()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipScroll { for (m in listOf("Ongkir", "Tip", "Lainnya")) Chip(m, row.reason == m) { row.reason = m } }
                        if (row.reason == "Lainnya") Field("Alasan", row.custom, { row.custom = it }, placeholder = "mis. biaya packing")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Field("Jumlah (Rp)", row.amount, { row.amount = digits(it) }, Modifier.weight(1f), keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
                            TextButton(onClick = { rows.removeAt(i) }) { Text("Hapus", color = c.accentText, fontSize = 12.sp) }
                        }
                    }
                }
                TextButton(onClick = { rows.add(IncRow("Ongkir", "", "")) }) { Text("+ Tambah pemasukan tambahan", color = c.accentText, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                Field("Catatan (opsional)", note, { note = it }, singleLine = false, placeholder = "mis. dibayar Pak Budi")
            }
        },
        confirmButton = {
            TextButton(
                enabled = method.isNotEmpty(),
                onClick = {
                    val mm = if (method == "Lainnya") custom.trim().ifBlank { "Lainnya" } else method
                    val inc = rows.mapNotNull {
                        val a = it.amount.toLongOrNull() ?: 0L
                        if (a <= 0) null else Income(if (it.reason == "Lainnya") it.custom.trim().ifBlank { "Lainnya" } else it.reason, a)
                    }
                    Repo.submitSettlement(r, mm, note, inc, me)
                    onClose()
                },
            ) { Text("Simpan", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } },
    )
}
