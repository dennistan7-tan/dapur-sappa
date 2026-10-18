package com.dapursappa.app.data

import java.time.LocalDate

/** Semua perubahan data lewat sini supaya otomatis tercatat di Riwayat. */
object Repo {
    fun log(me: Me, cat: String, title: String, detail: String) {
        Store.put(Audit(Store.newId(), System.currentTimeMillis(), "${me.name} · ${me.label}", cat, title, detail).toJson())
    }

    fun savePurchase(p: Purchase, old: Purchase?, me: Me) {
        Store.put(p.toJson())
        if (old == null) {
            log(me, "Belanja", "Tambah belanja: ${p.name}", purchaseLine(p))
        } else {
            val ch = ArrayList<String>()
            if (old.name != p.name) ch.add("Nama ${old.name} → ${p.name}")
            if (old.price != p.price) ch.add("Harga ${Fmt.rp(old.price)} → ${Fmt.rp(p.price)}")
            if (old.qty != p.qty || old.unit != p.unit) ch.add("Jumlah ${Fmt.num(old.qty)} ${old.unit} → ${Fmt.num(p.qty)} ${p.unit}")
            if (old.kind != p.kind) ch.add("Jenis ${old.kind.label} → ${p.kind.label}")
            if (old.useDate != p.useDate) ch.add("Untuk tanggal ${old.useDate} → ${p.useDate}")
            if (ch.isNotEmpty()) log(me, "Belanja", "Edit belanja: ${p.name}", ch.joinToString("\n"))
        }
    }

    fun deletePurchase(p: Purchase, me: Me) {
        Store.put(p.copy(deleted = true).toJson())
        log(me, "Belanja", "Hapus belanja: ${p.name}", purchaseLine(p) + " · Dihapus")
    }

    private fun purchaseLine(p: Purchase) =
        "${Fmt.num(p.qty)} ${p.unit} · ${Fmt.rp(p.price)} · ${p.kind.label}".replace("  ", " ")

    fun saveOrder(o: Order, old: Order?, me: Me) {
        Store.put(o.toJson())
        if (old == null) {
            log(me, "Order", "Tambah order ${o.customer}", orderLine(o))
        } else {
            val ch = ArrayList<String>()
            if (old.customer != o.customer) ch.add("Nama ${old.customer} → ${o.customer}")
            if (old.location != o.location) ch.add("Lokasi ${old.location} → ${o.location}")
            if (old.pax != o.pax) ch.add("Pax ${old.pax} → ${o.pax}")
            if (old.portionPrice != o.portionPrice) ch.add("Harga porsi ${Fmt.rp(old.portionPrice)} → ${Fmt.rp(o.portionPrice)}")
            if (old.extrasTotal != o.extrasTotal) ch.add("Tambahan ${Fmt.rp(old.extrasTotal)} → ${Fmt.rp(o.extrasTotal)}")
            if (old.paid != o.paid) ch.add(if (o.paid) "Belum bayar → Lunas" else "Lunas → Belum bayar")
            if (old.onlyExtra != o.onlyExtra) ch.add(if (o.onlyExtra) "Jadi lauk saja" else "Jadi porsi")
            if (old.date != o.date) ch.add("Tanggal ${old.date} → ${o.date}")
            if (old.remarks != o.remarks) ch.add("Remarks diubah")
            if (ch.isNotEmpty()) log(me, "Order", "Edit order ${o.customer}", ch.joinToString("\n"))
        }
    }

    fun deleteOrder(o: Order, me: Me) {
        Store.put(o.copy(deleted = true).toJson())
        log(me, "Order", "Hapus order ${o.customer}", orderLine(o) + " · Dihapus")
    }

    private fun orderLine(o: Order) =
        (if (o.onlyExtra) "Lauk saja" else "${o.pax} pax") + " · " + Fmt.rp(o.total) + " · " + (if (o.paid) "Lunas" else "Belum bayar")

    fun saveExpense(e: Expense, me: Me) {
        Store.put(e.toJson())
        log(me, "Pengeluaran", "Tambah pengeluaran: ${e.name}", "${Fmt.rp(e.amount)} · " + (if (e.monthly) "Profit Bulanan" else "Profit Harian"))
    }

    fun deleteExpense(e: Expense, me: Me) {
        Store.put(e.copy(deleted = true).toJson())
        log(me, "Pengeluaran", "Hapus pengeluaran: ${e.name}", "${Fmt.rp(e.amount)} · Dihapus")
    }

    fun saveMenu(newM: Menu, old: Menu?, me: Me) {
        Store.put(newM.toJson())
        val base = old ?: Menu.empty(newM.week)
        val monday = LocalDate.parse(newM.week)
        val ch = ArrayList<String>()
        for (i in 0 until 7) {
            val dn = Fmt.dayName(monday.plusDays(i.toLong()))
            if (base.days[i] != newM.days[i]) {
                ch.add("$dn: ${base.days[i].ifBlank { "Belum diisi" }} → ${newM.days[i].ifBlank { "Belum diisi" }}")
            }
            if (base.libur[i] != newM.libur[i]) ch.add("$dn: " + (if (newM.libur[i]) "Libur" else "Buka"))
        }
        if (base.price != newM.price) ch.add("Harga porsi ${Fmt.rp(base.price)} → ${Fmt.rp(newM.price)}")
        if (ch.isNotEmpty()) log(me, "Menu", "Ubah menu Week ${Fmt.weekNo(monday)}", ch.joinToString("\n"))
    }

    fun saveUsers(users: List<UserCfg>, me: Me) {
        Store.put(usersToJson(users))
        log(me, "User", "Ubah daftar user", users.joinToString(", ") { "${it.name} (${it.role})" })
    }
}
