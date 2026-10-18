package com.dapursappa.app.data

import android.content.Context
import com.dapursappa.app.Config

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("dapur_sappa", Context.MODE_PRIVATE)

    var codeOk: Boolean
        get() = sp.getBoolean("codeOk", false)
        set(v) = sp.edit().putBoolean("codeOk", v).apply()

    // -1 = belum memilih user
    var userIndex: Int
        get() = sp.getInt("userIndex", -1)
        set(v) = sp.edit().putInt("userIndex", v).apply()

    var isDev: Boolean
        get() = sp.getBoolean("isDev", false)
        set(v) = sp.edit().putBoolean("isDev", v).apply()

    // 0 = ikuti HP, 1 = terang, 2 = gelap
    var themeMode: Int
        get() = sp.getInt("themeMode", 0)
        set(v) = sp.edit().putInt("themeMode", v).apply()

    var devUser: String
        get() = sp.getString("devUser", null) ?: Config.DEV_DEFAULT_USER
        set(v) = sp.edit().putString("devUser", v).apply()

    var devHash: String
        get() = sp.getString("devHash", null) ?: Config.DEV_DEFAULT_HASH
        set(v) = sp.edit().putString("devHash", v).apply()

    var fbProject: String
        get() = (sp.getString("fbProject", null) ?: "").ifBlank { Config.FIREBASE_PROJECT_ID }
        set(v) = sp.edit().putString("fbProject", v.trim()).apply()

    var fbKey: String
        get() = (sp.getString("fbKey", null) ?: "").ifBlank { Config.FIREBASE_API_KEY }
        set(v) = sp.edit().putString("fbKey", v.trim()).apply()

    var lastPull: Long
        get() = sp.getLong("lastPull", 0L)
        set(v) = sp.edit().putLong("lastPull", v).apply()

    var lastSync: Long
        get() = sp.getLong("lastSync", 0L)
        set(v) = sp.edit().putLong("lastSync", v).apply()
}
