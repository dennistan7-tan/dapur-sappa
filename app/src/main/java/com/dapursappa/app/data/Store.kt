package com.dapursappa.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Penyimpanan lokal (offline-first). Semua data disimpan sebagai catatan JSON dan disinkron ke Firestore. */
object Store {
    private var file: File? = null
    private val recs = LinkedHashMap<String, JSONObject>()
    private val state = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = state

    fun newId(): String = UUID.randomUUID().toString()

    @Synchronized
    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "records.json")
        file = f
        if (f.exists()) {
            try {
                val arr = JSONArray(f.readText())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    recs[o.getString("id")] = o
                }
            } catch (e: Exception) {
                // abaikan file rusak
            }
        }
        rebuild()
    }

    @Synchronized
    fun put(o: JSONObject) {
        o.put("u", System.currentTimeMillis())
        o.put("_d", true)
        recs[o.getString("id")] = o
        save()
        rebuild()
        Sync.kick()
    }

    @Synchronized
    fun dirty(): List<JSONObject> =
        recs.values.filter { it.optBoolean("_d") }.map { JSONObject(it.toString()) }

    @Synchronized
    fun markPushed(id: String, u: Long) {
        val o = recs[id] ?: return
        if (o.optLong("u") == u) {
            o.remove("_d")
            save()
        }
    }

    /** Gabungkan catatan dari server. Yang lebih baru (u lebih besar) menang. */
    @Synchronized
    fun merge(remote: List<JSONObject>) {
        var changed = false
        for (r in remote) {
            val id = r.optString("id")
            if (id.isEmpty()) continue
            val local = recs[id]
            if (local == null || r.optLong("u") > local.optLong("u")) {
                r.remove("_d")
                recs[id] = r
                changed = true
            }
        }
        if (changed) {
            save()
            rebuild()
        }
    }

    private fun save() {
        val f = file ?: return
        try {
            val arr = JSONArray()
            recs.values.forEach { arr.put(it) }
            f.writeText(arr.toString())
        } catch (e: Exception) {
            // abaikan
        }
    }

    private fun rebuild() {
        val purchases = ArrayList<Purchase>()
        val orders = ArrayList<Order>()
        val menus = HashMap<String, Menu>()
        val expenses = ArrayList<Expense>()
        val audits = ArrayList<Audit>()
        var users = defaultUsers()
        for (o in recs.values) {
            try {
                when (o.optString("t")) {
                    "p" -> { val x = Purchase.from(o); if (!x.deleted) purchases.add(x) }
                    "o" -> { val x = Order.from(o); if (!x.deleted) orders.add(x) }
                    "m" -> { val x = Menu.from(o); menus[x.week] = x }
                    "e" -> { val x = Expense.from(o); if (!x.deleted) expenses.add(x) }
                    "a" -> { audits.add(Audit.from(o)) }
                    "c" -> { users = usersFromJson(o) }
                }
            } catch (e: Exception) {
                // lewati catatan rusak
            }
        }
        audits.sortByDescending { it.ts }
        state.value = AppData(purchases, orders, menus, expenses, audits, users)
    }
}
