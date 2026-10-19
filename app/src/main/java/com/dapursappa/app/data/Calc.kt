package com.dapursappa.app.data

import java.time.LocalDate

data class KemasanLine(val name: String, val unitPrice: Double, val pcs: Int, val cost: Long)

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
) {
    val modal: Long get() = bahanCost + kemasanCost
}

object Calc {
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
        if (pax > 0) {
            val groups = d.purchases.filter { it.kind == Kind.KEMASAN && it.qty > 0 && it.date <= ds }
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
        return MonthCalc(
            first, days, rev, days.sumOf { it.bahanCost }, days.sumOf { it.kemasanCost },
            days.sumOf { it.expCost }, lines.toList(), monthlyCost,
            days.sumOf { it.profit } - monthlyCost, days.sumOf { it.pax }, unpaid,
        )
    }
}
