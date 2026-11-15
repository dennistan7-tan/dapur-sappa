package com.dapursappa.app.data

import java.time.LocalDate

data class KemasanLine(val name: String, val unitPrice: Double, val pcs: Int, val cost: Long)

/** Stok kemasan stok (bulanan): dibeli - terpakai (1 pcs per porsi). Kemasan spesial harian tidak ikut. */
data class KemasanStock(val name: String, val unit: String, val bought: Double, val used: Double) {
    val left: Double get() = bought - used
}

data class DayCalc(
    val date: LocalDate,
    val menuName: String,
    val libur: Boolean,
    val orders: List<Order>,
    val pax: Int,
    val revenue: Long,
    val bahan: List<Purchase>,
    val bahanCost: Long,
    val kemasan: List<KemasanLine>,
    val kemasanCost: Long,
    val expenses: List<Expense>,
    val expCost: Long,
) {
    val cost: Long get() = bahanCost + kemasanCost
    val profit: Long get() = revenue - cost - expCost
    val costPerPax: Double get() = if (pax > 0) cost.toDouble() / pax else 0.0
}

data class MonthCalc(
    val month: LocalDate,
    val days: List<DayCalc>,
    val revenue: Long,
    val bahanCost: Long,
    val kemasanCost: Long,
    val expHarian: Long,
    val monthlyLines: List<Pair<String, Long>>,
    val monthlyCost: Long,
    val profit: Long,
    val pax: Int,
    val unpaidOrders: List<Order>,
    val extraIncome: Long = 0,
) {
    val modal: Long get() = bahanCost + kemasanCost
}

/** Satu baris di layar Konfirmasi Pembayaran: customer x minggu. */
data class ConfCust(
    val key: String, val name: String, val week: LocalDate, val orders: List<Order>,
    val bill: Long, val unpaid: Long, val settlement: Settlement?,
) { val paid: Boolean get() = unpaid == 0L }

data class ConfWeek(val week: LocalDate, val rows: List<ConfCust>) {
    val bill: Long get() = rows.sumOf { it.bill }
    val unpaid: Long get() = rows.sumOf { it.unpaid }
}

data class MenuStat(val name: String, val count: Int, val pax: Int, val profit: Long, val dates: List<String>) {
    val mpp: Long get() = if (pax > 0) Math.round(profit.toDouble() / pax) else 0L
}

data class MenuAnalysis(val days: Int, val pax: Int, val revenue: Long, val profit: Long, val items: List<MenuStat>) {
    val mpp: Long get() = if (pax > 0) Math.round(profit.toDouble() / pax) else 0L
}

data class BuyUnit(val unit: String, val qty: Double, val spend: Long) {
    val unitPrice: Long get() = if (qty > 0) Math.round(spend / qty) else 0L
}

data class BuyItem(val name: String, val kind: Kind, val times: Int, val spend: Long, val units: List<BuyUnit>)

data class CustWeek(
    val week: LocalDate, val orders: List<Order>, val pax: Int, val revenue: Long,
    val unpaid: List<Order>, val methods: List<String>,
) {
    val unpaidSum: Long get() = unpaid.sumOf { it.total }
    val paid: Boolean get() = unpaid.isEmpty()
}

data class CustCalc(
    val key: String, val name: String, val orders: List<Order>, val pax: Int,
    val revenue: Long, val profit: Long, val weeks: List<CustWeek>,
) {
    val unpaidSum: Long get() = weeks.sumOf { it.unpaidSum }
    val allPaid: Boolean get() = unpaidSum == 0L
}

data class CustSummary(val list: List<CustCalc>) {
    val customers: Int get() = list.size
    val orders: Int get() = list.sumOf { it.orders.size }
    val pax: Int get() = list.sumOf { it.pax }
    val revenue: Long get() = list.sumOf { it.revenue }
    val profit: Long get() = list.sumOf { it.profit }
    val unpaid: Long get() = list.sumOf { it.unpaidSum }
}

object Calc {
    /** Laporan per customer sebulan. Profit = omzet dikurangi modal bahan dan kemasan per porsi di hari order. */
    fun customers(d: AppData, any: LocalDate): CustSummary {
        val prefix = any.withDayOfMonth(1).toString().substring(0, 7)
        val cache = HashMap<String, DayCalc>()
        val groups = d.orders.filter { it.date.startsWith(prefix) }.groupBy { it.customer.trim().lowercase() }
        val list = groups.map { (key, os) ->
            var cost = 0.0
            for (o in os) cost += o.portions * cache.getOrPut(o.date) { day(d, LocalDate.parse(o.date)) }.costPerPax
            val revenue = os.sumOf { it.total }
            val weeks = os.groupBy { Fmt.mondayOf(LocalDate.parse(it.date)) }.toSortedMap().map { (wk, wos) ->
                val billable = wos.filter { it.total > 0 }
                CustWeek(
                    wk, wos.sortedBy { it.date }, wos.sumOf { it.portions }, wos.sumOf { it.total },
                    billable.filter { !it.paid },
                    billable.filter { it.paid && it.payMethod.isNotBlank() }.map { it.payMethod }.distinct(),
                )
            }
            CustCalc(key, os.first().customer.trim(), os, os.sumOf { it.portions }, revenue, Math.round(revenue - cost), weeks)
        }.sortedWith(compareByDescending<CustCalc> { it.revenue }.thenBy { it.name })
        return CustSummary(list)
    }

    /** Minggu (Senin) yang masuk bulan ini, tiap minggu berisi tagihan per customer. */
    fun confirmWeeks(d: AppData, any: LocalDate): List<ConfWeek> {
        val first = any.withDayOfMonth(1)
        val out = ArrayList<ConfWeek>()
        var mon = Fmt.mondayOf(first)
        if (mon.month != first.month) mon = mon.plusWeeks(1)
        while (mon.month == first.month) {
            val a = mon.toString(); val b = mon.plusDays(6).toString()
            val groups = d.orders.filter { it.date in a..b && it.total > 0 }.groupBy { it.customer.trim().lowercase() }
            val rows = groups.map { (key, os) ->
                ConfCust(key, os.first().customer.trim(), mon, os.sortedBy { it.date }, os.sumOf { it.total },
                    os.filter { !it.paid }.sumOf { it.total }, d.settlements.firstOrNull { it.id == Settlement.idOf(key, a) })
            }.sortedBy { it.name.lowercase() }
            out.add(ConfWeek(mon, rows))
            mon = mon.plusWeeks(1)
        }
        return out
    }

    /** Analisis menu (developer): margin per pax per hari, dirata-rata per menu. Hanya hari berjalan dengan modal terinput. */
    fun menuAnalysis(d: AppData, today: LocalDate): MenuAnalysis {
        val dates = (d.orders.map { it.date } + d.purchases.map { it.useDate }).filter { it <= today.toString() }.distinct().sorted()
        var days = 0; var pax = 0; var rev = 0L; var prof = 0L
        val acc = LinkedHashMap<String, MutableList<DayCalc>>()
        val names = HashMap<String, String>()
        for (ds in dates) {
            val dc = day(d, LocalDate.parse(ds))
            if (dc.libur && dc.pax == 0 || dc.pax == 0 || dc.cost <= 0) continue
            days++; pax += dc.pax; rev += dc.revenue; prof += dc.profit
            val seen = HashSet<String>()
            for (line in dc.menuName.lines().map { it.trim() }.filter { it.isNotEmpty() }) {
                val k = line.lowercase()
                if (k == "nasi putih" || k == "nasi" || !seen.add(k)) continue
                names.putIfAbsent(k, line); acc.getOrPut(k) { ArrayList() }.add(dc)
            }
        }
        val items = acc.map { (k, l) -> MenuStat(names[k] ?: k, l.size, l.sumOf { it.pax }, l.sumOf { it.profit }, l.map { it.date.toString() }) }
        return MenuAnalysis(days, pax, rev, prof, items)
    }

    /** Daftar bahan belanja, satu baris per nama (urut abjad), dengan total qty, total biaya dan harga per satuan. */
    fun buyList(d: AppData): List<BuyItem> {
        val g = LinkedHashMap<String, MutableList<Purchase>>()
        for (p in d.purchases) g.getOrPut(p.name.trim().lowercase()) { ArrayList() }.add(p)
        return g.map { (_, l) ->
            val units = l.groupBy { it.unit.trim().lowercase() }.map { (u, x) -> BuyUnit(u, x.sumOf { it.qty }, x.sumOf { it.price }) }
                .sortedByDescending { it.spend }
            BuyItem(l.first().name.trim().replaceFirstChar { it.uppercase() }, l.first().kind, l.size, l.sumOf { it.price }, units)
        }.sortedBy { it.name.lowercase() }
    }

    fun menuOf(d: AppData, date: LocalDate): Menu? = d.menus[Fmt.mondayOf(date).toString()]

    /** Menu mentah: tiap baris dipisah newline (maks 3 baris). */
    fun menuRaw(d: AppData, date: LocalDate): String =
        menuOf(d, date)?.days?.getOrNull(date.dayOfWeek.value - 1).orEmpty()

    /** Menu satu baris, baris dipisah " · ". */
    fun menuName(d: AppData, date: LocalDate): String =
        menuRaw(d, date).lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")

    fun isLibur(d: AppData, date: LocalDate): Boolean {
        val m = menuOf(d, date)
        return m?.libur?.getOrNull(date.dayOfWeek.value - 1) ?: (date.dayOfWeek.value == 7)
    }

    fun defaultPrice(d: AppData, date: LocalDate): Long = menuOf(d, date)?.price ?: lastPrice(d)

    private fun lastPrice(d: AppData): Long =
        d.menus.values.maxByOrNull { it.week }?.price ?: 20000L

    fun day(d: AppData, date: LocalDate): DayCalc {
        val ds = date.toString()
        val orders = d.orders.filter { it.date == ds }.sortedWith(compareBy({ it.sortKey }, { it.customer.lowercase() }))
        val pax = orders.sumOf { it.portions }
        val revenue = orders.sumOf { it.total }
        val bahan = d.purchases.filter { it.kind == Kind.BAHAN && it.useDate == ds }
        val bahanCost = bahan.sumOf { it.price }

        val kemasan = ArrayList<KemasanLine>()
        val special = d.purchases.filter { it.kind == Kind.KEMASAN && it.daily && it.useDate == ds }
        if (special.isNotEmpty()) {
            // Hari dengan kemasan spesial: hanya kemasan spesial yang dihitung, stok bulanan tidak terpotong.
            for (p in special) kemasan.add(KemasanLine(p.name, if (p.qty > 0) p.price / p.qty else p.price.toDouble(), p.qty.toInt(), p.price))
        } else if (pax > 0) {
            val groups = d.purchases.filter { it.kind == Kind.KEMASAN && !it.daily && it.qty > 0 && it.date <= ds }
                .groupBy { it.name.lowercase().trim() }
            for ((_, list) in groups) {
                val latest = list.maxByOrNull { it.date } ?: continue
                val unit = latest.price / latest.qty
                kemasan.add(KemasanLine(latest.name, unit, pax, Math.round(unit * pax)))
            }
        }
        val kemasanCost = kemasan.sumOf { it.cost }
        val exps = d.expenses.filter { it.date == ds && !it.monthly }
        return DayCalc(
            date, menuRaw(d, date), isLibur(d, date), orders, pax, revenue,
            bahan, bahanCost, kemasan, kemasanCost, exps, exps.sumOf { it.amount },
        )
    }

    /** Stok sisa kemasan bulanan sampai hari ini. */
    fun kemasanStock(d: AppData, today: LocalDate): List<KemasanStock> {
        val ts = today.toString()
        val skip = d.purchases.filter { it.kind == Kind.KEMASAN && it.daily }.map { it.useDate }.toSet()
        val paxByDate = d.orders.filter { it.date <= ts && it.date !in skip }.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.portions } }
        val groups = d.purchases.filter { it.kind == Kind.KEMASAN && !it.daily && it.qty > 0 }.groupBy { it.name.lowercase().trim() }
        return groups.values.map { list ->
            val first = list.minOf { it.date }
            val latest = list.maxByOrNull { it.date }!!
            val used = paxByDate.filter { it.key >= first }.values.sum()
            KemasanStock(latest.name, latest.unit.ifBlank { "pcs" }, list.sumOf { it.qty }, used.toDouble())
        }.sortedBy { it.name.lowercase() }
    }

    fun week(d: AppData, monday: LocalDate): List<DayCalc> = List(7) { day(d, monday.plusDays(it.toLong())) }

    fun month(d: AppData, any: LocalDate): MonthCalc {
        val first = any.withDayOfMonth(1)
        val len = first.lengthOfMonth()
        val days = List(len) { day(d, first.plusDays(it.toLong())) }
        val prefix = first.toString().substring(0, 7)
        val lines = LinkedHashMap<String, Long>()
        for (p in d.purchases) {
            if (p.kind == Kind.BULANAN && p.date.startsWith(prefix)) {
                val k = p.name.trim()
                lines[k] = (lines[k] ?: 0L) + p.price
            }
        }
        for (e in d.expenses) {
            if (e.monthly && e.date.startsWith(prefix)) {
                val k = e.name.trim()
                lines[k] = (lines[k] ?: 0L) + e.amount
            }
        }
        val monthlyCost = lines.values.sum()
        val rev = days.sumOf { it.revenue }
        val unpaid = d.orders.filter { it.date.startsWith(prefix) && !it.paid && it.total > 0 }
        val extra = d.settlements.filter { it.week.startsWith(prefix) }.sumOf { it.extra }
        return MonthCalc(
            first, days, rev, days.sumOf { it.bahanCost }, days.sumOf { it.kemasanCost },
            days.sumOf { it.expCost }, lines.toList(), monthlyCost,
            days.sumOf { it.profit } - monthlyCost + extra, days.sumOf { it.pax }, unpaid, extra,
        )
    }
}
