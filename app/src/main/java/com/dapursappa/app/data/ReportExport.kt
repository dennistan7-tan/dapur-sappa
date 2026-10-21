package com.dapursappa.app.data

import java.time.LocalDate

/** Susun semua data laporan jadi sheet Excel: bulan, minggu, hari, pengeluaran (record + kategori), order. */
object ReportExport {
    private fun pct(p: Long, r: Long): Any? = if (r > 0) XC(p.toDouble() / r, 3) else ""
    private fun per(a: Long, n: Int): Any? = if (n > 0) Math.round(a.toDouble() / n) else ""

    private fun dateRange(d: AppData): Pair<LocalDate, LocalDate> {
        val ds = ArrayList<String>()
        d.orders.forEach { ds.add(it.date) }
        d.purchases.forEach { ds.add(it.date); if (it.kind == Kind.BAHAN) ds.add(it.useDate) }
        d.expenses.forEach { ds.add(it.date) }
        val ok = ds.mapNotNull { try { LocalDate.parse(it) } catch (e: Exception) { null } }
        val today = LocalDate.now()
        val a = ok.minOrNull() ?: today
        val b = (ok + today).maxOrNull() ?: today
        return a to b
    }

    fun sheets(d: AppData): List<XSheet> {
        val (from, to) = dateRange(d)
        val out = ArrayList<XSheet>()

        // ===== Per bulan
        val mh = listOf<Any?>("Bulan", "Total pax", "Omzet", "Belanja bahan", "Kemasan", "Modal (bahan+kemasan)", "Pengeluaran harian",
            "Gross profit", "Margin gross", "Biaya bulanan", "Profit bersih", "Margin bersih", "Modal per porsi", "Harga jual rata-rata per porsi")
        val mrows = ArrayList<List<Any?>>(); mrows.add(mh)
        var m = from.withDayOfMonth(1)
        var tp = 0; var tr = 0L; var tb = 0L; var tk = 0L; var te = 0L; var tm = 0L
        while (!m.isAfter(to)) {
            val mc = Calc.month(d, m)
            val gross = mc.revenue - mc.modal - mc.expHarian
            if (mc.revenue > 0 || mc.modal > 0 || mc.expHarian > 0 || mc.monthlyCost > 0) {
                mrows.add(listOf(Fmt.monthName(m), mc.pax, mc.revenue, mc.bahanCost, mc.kemasanCost, mc.modal, mc.expHarian,
                    gross, pct(gross, mc.revenue), mc.monthlyCost, mc.profit, pct(mc.profit, mc.revenue), per(mc.modal, mc.pax), per(mc.revenue, mc.pax)))
                tp += mc.pax; tr += mc.revenue; tb += mc.bahanCost; tk += mc.kemasanCost; te += mc.expHarian; tm += mc.monthlyCost
            }
            m = m.plusMonths(1)
        }
        val tmod = tb + tk; val tg = tr - tmod - te
        mrows.add(listOf(XC("TOTAL", 5), XC(tp, 4), XC(tr, 4), XC(tb, 4), XC(tk, 4), XC(tmod, 4), XC(te, 4), XC(tg, 4),
            XC(if (tr > 0) tg.toDouble() / tr else "", 3), XC(tm, 4), XC(tg - tm, 4), XC(if (tr > 0) (tg - tm).toDouble() / tr else "", 3),
            XC(if (tp > 0) Math.round(tmod.toDouble() / tp) else "", 4), XC(if (tp > 0) Math.round(tr.toDouble() / tp) else "", 4)))
        out.add(XSheet("Per Bulan", listOf(18, 10, 14, 14, 12, 16, 16, 14, 10, 14, 14, 10, 14, 18), mrows))

        // ===== Per minggu
        val wh = listOf<Any?>("Minggu ke", "Periode", "Total pax", "Omzet", "Belanja bahan", "Kemasan", "Modal (bahan+kemasan)",
            "Pengeluaran harian", "Gross profit", "Margin gross", "Modal per porsi", "Harga jual rata-rata per porsi")
        val wrows = ArrayList<List<Any?>>(); wrows.add(wh)
        var wk = Fmt.mondayOf(from)
        while (!wk.isAfter(to)) {
            val days = Calc.week(d, wk)
            val rev = days.sumOf { it.revenue }; val pax = days.sumOf { it.pax }
            val bh = days.sumOf { it.bahanCost }; val km = days.sumOf { it.kemasanCost }; val ex = days.sumOf { it.expCost }
            if (rev > 0 || bh > 0 || km > 0 || ex > 0) {
                val g = rev - bh - km - ex
                wrows.add(listOf("W${Fmt.weekNo(wk)} ${wk.year}", Fmt.range(wk, wk.plusDays(6)), pax, rev, bh, km, bh + km, ex, g,
                    pct(g, rev), per(bh + km, pax), per(rev, pax)))
            }
            wk = wk.plusWeeks(1)
        }
        out.add(XSheet("Per Minggu", listOf(12, 28, 10, 14, 14, 12, 16, 16, 14, 10, 14, 18), wrows))

        // ===== Per hari
        val dh = listOf<Any?>("Tanggal", "Hari", "Menu", "Total pax", "Omzet", "Belanja bahan (harga bahan hari itu)", "Kemasan", "Modal",
            "Pengeluaran harian", "Gross profit", "Margin gross", "Modal per porsi", "Harga jual per porsi")
        val drows = ArrayList<List<Any?>>(); drows.add(dh)
        var day = from
        while (!day.isAfter(to)) {
            val dc = Calc.day(d, day)
            if (dc.pax > 0 || dc.revenue > 0 || dc.bahanCost > 0 || dc.expCost > 0) {
                drows.add(listOf(day.toString(), Fmt.dayName(day), dc.menuName.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · "),
                    dc.pax, dc.revenue, dc.bahanCost, dc.kemasanCost, dc.cost, dc.expCost, dc.profit, pct(dc.profit, dc.revenue),
                    per(dc.cost, dc.pax), per(dc.revenue, dc.pax)))
            }
            day = day.plusDays(1)
        }
        out.add(XSheet("Per Hari", listOf(12, 10, 36, 10, 14, 18, 12, 14, 16, 14, 10, 14, 16), drows))

        // ===== Semua pengeluaran (record + kategori)
        class Rec(val date: String, val cat: String, val name: String, val qty: String, val amount: Long, val use: String, val charge: String, val by: Int)
        val recs = ArrayList<Rec>()
        for (p in d.purchases) {
            val charge = when (p.kind) {
                Kind.BAHAN -> "Modal harian (bahan)"; Kind.KEMASAN -> "Modal harian (kemasan)"
                Kind.BULANAN -> "Biaya bulanan"; Kind.LAIN -> "Profit harian"
            }
            recs.add(Rec(p.date, p.kind.label, p.name, Fmt.num(p.qty) + (if (p.unit.isNotBlank()) " " + p.unit else ""), p.price,
                if (p.kind == Kind.BAHAN) p.useDate else "", charge, p.by))
        }
        for (e in d.expenses) {
            recs.add(Rec(e.date, if (e.monthly) "Biaya bulanan" else "Pengeluaran lain", e.name, "", e.amount, "",
                if (e.monthly) "Biaya bulanan" else "Profit harian", e.by))
        }
        recs.sortWith(compareBy({ it.date }, { it.cat }))
        val rrows = ArrayList<List<Any?>>()
        rrows.add(listOf("Tanggal", "Kategori", "Nama / item", "Jumlah", "Total (Rp)", "Dipakai untuk tanggal", "Dibebankan ke", "Dicatat oleh"))
        for (r in recs) rrows.add(listOf(r.date, r.cat, r.name, r.qty, r.amount, r.use, r.charge, if (r.by < 0) "Developer" else d.users.getOrNull(r.by)?.name ?: "User"))
        rrows.add(listOf(XC("TOTAL", 5), XC("", 5), XC("", 5), XC("", 5), XC(recs.sumOf { it.amount }, 4)))
        out.add(XSheet("Pengeluaran", listOf(12, 20, 30, 12, 14, 18, 24, 14), rrows))

        // ===== Order
        val orows = ArrayList<List<Any?>>()
        orows.add(listOf("Tanggal", "Jam antar", "Waktu", "Customer", "Lokasi", "Pax", "Harga/porsi", "Tambahan", "Total", "Status bayar", "Remarks"))
        for (o in d.orders.sortedWith(compareBy({ it.date }, { it.sortKey }, { it.customer.lowercase() }))) {
            orows.add(listOf(o.date, o.time, o.slot, o.customer, o.location, o.portions, o.portionPrice, o.extrasTotal, o.total,
                if (o.paid) "Lunas" else "Belum bayar", o.remarks))
        }
        out.add(XSheet("Order", listOf(12, 10, 10, 22, 18, 8, 12, 12, 14, 14, 24), orows))
        return out
    }
}
