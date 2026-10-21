package com.dapursappa.app.data

import org.json.JSONArray
import org.json.JSONObject

enum class Kind(val label: String) {
    BAHAN("Bahan harian"),
    KEMASAN("Kemasan"),
    BULANAN("Biaya bulanan"),
    LAIN("Pengeluaran lain"),
}

data class Me(val idx: Int, val isDev: Boolean, val name: String, val role: String) {
    // Semua user punya akses penuh (dapur dan admin digabung).
    val canAdmin: Boolean get() = true
    val label: String get() = if (isDev) "Developer" else "User"
}

data class UserCfg(val name: String, val role: String)

data class Purchase(
    val id: String,
    val date: String,
    val useDate: String,
    val raw: String,
    val name: String,
    val qty: Double,
    val unit: String,
    val price: Long,
    val kind: Kind,
    val by: Int,
    val deleted: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("t", "p").put("id", id).put("date", date).put("useDate", useDate)
        .put("raw", raw).put("name", name).put("qty", qty).put("unit", unit)
        .put("price", price).put("kind", kind.name).put("by", by).put("del", deleted)

    companion object {
        fun from(j: JSONObject): Purchase {
            val kind = try { Kind.valueOf(j.optString("kind")) } catch (e: Exception) { Kind.BAHAN }
            return Purchase(
                j.getString("id"), j.optString("date"), j.optString("useDate", j.optString("date")),
                j.optString("raw"), j.optString("name"), j.optDouble("qty", 1.0), j.optString("unit"),
                j.optLong("price"), kind, j.optInt("by", -1), j.optBoolean("del"),
            )
        }
    }
}

data class Extra(val name: String, val qty: Int, val unitPrice: Long, val note: String) {
    val total: Long get() = qty * unitPrice
}

data class Order(
    val id: String,
    val date: String,
    val customer: String,
    val location: String,
    val pax: Int,
    val remarks: String,
    val portionPrice: Long,
    val extras: List<Extra>,
    val paid: Boolean,
    val onlyExtra: Boolean,
    val by: Int,
    val deleted: Boolean = false,
    val slot: String = "",
    val time: String = "",
    val payMethod: String = "",
) {
    val delivery: String get() = if (slot.isNotBlank()) slot + (if (time.isNotBlank()) " · $time" else "") else time
    /** Kunci urut pengantaran: jam yang diisi, atau jam bawaan slot; tanpa jam di paling bawah. */
    val sortKey: String get() = when {
        time.isNotBlank() -> time
        slot == "PAGI" -> "07:00"
        slot == "SIANG" -> "12:00"
        slot == "SORE" -> "16:00"
        slot == "MALAM" -> "19:00"
        else -> "99:99"
    }
    val portions: Int get() = if (onlyExtra) 0 else pax
    val extrasTotal: Long get() = extras.sumOf { it.total }
    val total: Long get() = portions * portionPrice + extrasTotal

    fun toJson(): JSONObject {
        val arr = JSONArray()
        for (e in extras) {
            arr.put(JSONObject().put("n", e.name).put("q", e.qty).put("p", e.unitPrice).put("c", e.note))
        }
        return JSONObject()
            .put("t", "o").put("id", id).put("date", date).put("customer", customer)
            .put("location", location).put("pax", pax).put("remarks", remarks)
            .put("price", portionPrice).put("extras", arr).put("paid", paid)
            .put("only", onlyExtra).put("by", by).put("del", deleted)
            .put("slot", slot).put("time", time).put("pm", payMethod)
    }

    companion object {
        fun from(j: JSONObject): Order {
            val list = ArrayList<Extra>()
            val arr = j.optJSONArray("extras")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(Extra(o.optString("n"), o.optInt("q", 1), o.optLong("p"), o.optString("c")))
                }
            }
            return Order(
                j.getString("id"), j.optString("date"), j.optString("customer"), j.optString("location"),
                j.optInt("pax"), j.optString("remarks"), j.optLong("price", 20000), list,
                j.optBoolean("paid"), j.optBoolean("only"), j.optInt("by", -1), j.optBoolean("del"),
                j.optString("slot"), j.optString("time"), j.optString("pm"),
            )
        }
    }
}

data class Menu(val week: String, val days: List<String>, val libur: List<Boolean>, val price: Long) {
    val id: String get() = "menu-$week"

    fun toJson(): JSONObject {
        val d = JSONArray(); days.forEach { d.put(it) }
        val l = JSONArray(); libur.forEach { l.put(it) }
        return JSONObject().put("t", "m").put("id", id).put("week", week)
            .put("days", d).put("libur", l).put("price", price)
    }

    companion object {
        fun from(j: JSONObject): Menu {
            val d = ArrayList<String>(); val l = ArrayList<Boolean>()
            val da = j.optJSONArray("days"); val la = j.optJSONArray("libur")
            for (i in 0 until 7) {
                d.add(if (da != null && i < da.length()) da.optString(i) else "")
                l.add(if (la != null && i < la.length()) la.optBoolean(i) else i == 6)
            }
            return Menu(j.optString("week"), d, l, j.optLong("price", 20000))
        }

        fun empty(week: String, price: Long = 20000) =
            Menu(week, List(7) { "" }, List(7) { it == 6 }, price)
    }
}

data class Expense(
    val id: String,
    val date: String,
    val name: String,
    val amount: Long,
    val monthly: Boolean,
    val by: Int,
    val deleted: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().put("t", "e").put("id", id).put("date", date)
        .put("name", name).put("amount", amount).put("monthly", monthly).put("by", by).put("del", deleted)

    companion object {
        fun from(j: JSONObject) = Expense(
            j.getString("id"), j.optString("date"), j.optString("name"), j.optLong("amount"),
            j.optBoolean("monthly"), j.optInt("by", -1), j.optBoolean("del"),
        )
    }
}

data class Audit(
    val id: String,
    val ts: Long,
    val who: String,
    val cat: String,
    val title: String,
    val detail: String,
) {
    fun toJson(): JSONObject = JSONObject().put("t", "a").put("id", id).put("ts", ts)
        .put("who", who).put("cat", cat).put("title", title).put("detail", detail)

    companion object {
        fun from(j: JSONObject) = Audit(
            j.getString("id"), j.optLong("ts"), j.optString("who"), j.optString("cat"),
            j.optString("title"), j.optString("detail"),
        )
    }
}

fun defaultUsers(): List<UserCfg> = List(2) { UserCfg("User ${it + 1}", "User") }

fun usersToJson(users: List<UserCfg>): JSONObject {
    val arr = JSONArray()
    for (u in users) arr.put(JSONObject().put("name", u.name).put("role", u.role))
    return JSONObject().put("t", "c").put("id", "cfg").put("users", arr)
}

fun usersFromJson(j: JSONObject): List<UserCfg> {
    val arr = j.optJSONArray("users") ?: return defaultUsers()
    val out = ArrayList<UserCfg>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        if (out.size < 2) out.add(UserCfg(o.optString("name"), "User"))
    }
    return if (out.isEmpty()) defaultUsers() else out
}

data class AppData(
    val purchases: List<Purchase> = emptyList(),
    val orders: List<Order> = emptyList(),
    val menus: Map<String, Menu> = emptyMap(),
    val expenses: List<Expense> = emptyList(),
    val audits: List<Audit> = emptyList(),
    val users: List<UserCfg> = defaultUsers(),
)
