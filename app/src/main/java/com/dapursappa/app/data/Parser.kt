package com.dapursappa.app.data

data class Parsed(
    val name: String,
    val qty: Double,
    val unit: String,
    val price: Long,
    val kind: Kind,
) {
    val ok: Boolean get() = name.isNotBlank() && price > 0
}

/** Membaca teks bebas belanja, mis. "ayam dada fillet 36rb 1kg" atau "selada 3 ikat 9rb". Urutan bebas. */
object Parser {
    private val PRICE_SUFFIX = mapOf(
        "rb" to 1_000.0, "ribu" to 1_000.0, "k" to 1_000.0, "jt" to 1_000_000.0, "juta" to 1_000_000.0,
    )

    private val UNITS = mapOf(
        "kg" to "kg", "kilo" to "kg", "kgs" to "kg",
        "g" to "g", "gr" to "g", "gram" to "g", "ons" to "ons",
        "ml" to "ml", "l" to "l", "ltr" to "l", "liter" to "l",
        "pcs" to "pcs", "pc" to "pcs", "biji" to "pcs", "buah" to "pcs", "bh" to "pcs",
        "butir" to "butir", "ikat" to "ikat", "bungkus" to "bungkus", "bks" to "bungkus",
        "pack" to "pack", "pak" to "pack", "papan" to "papan", "sisir" to "sisir",
        "lembar" to "lembar", "botol" to "botol", "kotak" to "kotak", "dus" to "dus",
        "karung" to "karung", "siung" to "siung", "porsi" to "porsi", "ekor" to "ekor",
        "potong" to "potong", "sachet" to "sachet", "renteng" to "renteng", "batang" to "batang",
        "tabung" to "tabung", "roll" to "roll", "gulung" to "gulung", "kaleng" to "kaleng",
        "ruas" to "ruas", "tangkai" to "tangkai", "genggam" to "genggam", "bonggol" to "bonggol",
        "liter" to "l", "slop" to "slop", "bal" to "bal", "box" to "box",
    )

    private val DROP = setOf("rp", "rp.", "seharga", "harga", "beli", "isi", "x", "@", "total", "sebanyak")

    private val KEMASAN_WORDS = listOf(
        "sterofoam", "styrofoam", "steroform", "stereofoam", "styrofom", "sterofom", "mika", "cup",
        "kertas nasi", "sendok", "garpu", "sedotan", "wadah", "paper bowl", "box", "kotak nasi",
        "tempat makan", "alumunium foil", "aluminium foil", "foil",
    )
    private val BULANAN_WORDS = listOf(
        "kantong", "kresek", "elpiji", "lpg", "tisu", "tissue", "karet", "tali", "sabun", "spons",
        "lakban", "isolasi", "plastik", "stiker", "label", "korek",
    )

    fun detectKind(name: String): Kind {
        val n = name.lowercase()
        val words = n.split(" ").filter { it.isNotEmpty() }
        if (words.contains("gas")) return Kind.BULANAN
        for (w in KEMASAN_WORDS) if (n.contains(w)) return Kind.KEMASAN
        for (w in BULANAN_WORDS) if (n.contains(w)) return Kind.BULANAN
        return Kind.BAHAN
    }

    private fun parseNum(s: String): Double? {
        if (s.isEmpty()) return null
        val t = s.trim('.', ',')
        if (t.isEmpty()) return null
        if (Regex("^\\d{1,3}([.,]\\d{3})+$").matches(t)) {
            return t.replace(".", "").replace(",", "").toDoubleOrNull()
        }
        return t.replace(',', '.').toDoubleOrNull()
    }

    private val NUM_ONLY = Regex("^[\\d.,]*\\d$")
    private val NUM_LETTERS = Regex("^(rp\\.?)?([\\d.,]*\\d)([a-z]*)$")

    fun parse(text: String): Parsed? {
        val clean = text.lowercase().replace("\n", " ").trim()
        if (clean.isEmpty()) return null
        val tokens = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }

        val nameParts = ArrayList<String>()
        var explicitPrice: Long? = null
        var qty: Double? = null
        var unit = ""
        val bare = ArrayList<Double>()

        var i = 0
        while (i < tokens.size) {
            val tk = tokens[i]
            val next = if (i + 1 < tokens.size) tokens[i + 1] else null

            if ((tk == "rp" || tk == "rp.") && next != null && NUM_ONLY.matches(next)) {
                val v = parseNum(next)
                if (v != null) {
                    val after = if (i + 2 < tokens.size) tokens[i + 2] else null
                    val mult = if (after != null) PRICE_SUFFIX[after] else null
                    if (mult != null) {
                        explicitPrice = Math.round(v * mult); i += 3
                    } else {
                        explicitPrice = Math.round(v); i += 2
                    }
                    continue
                }
            }

            val m = NUM_LETTERS.matchEntire(tk)
            if (m != null) {
                val hasRp = m.groupValues[1].isNotEmpty()
                val v = parseNum(m.groupValues[2])
                val letters = m.groupValues[3]
                if (v != null) {
                    if (letters.isEmpty()) {
                        val mult = if (next != null) PRICE_SUFFIX[next] else null
                        val u = if (next != null) UNITS[next] else null
                        if (mult != null) {
                            explicitPrice = Math.round(v * mult); i += 2; continue
                        } else if (u != null && !hasRp) {
                            if (qty == null) { qty = v; unit = u } else nameParts.add(tk)
                            i += 2; continue
                        } else if (hasRp) {
                            explicitPrice = Math.round(v); i++; continue
                        } else {
                            bare.add(v); i++; continue
                        }
                    } else {
                        val mult = PRICE_SUFFIX[letters]
                        val u = UNITS[letters]
                        if (mult != null) {
                            explicitPrice = Math.round(v * mult); i++; continue
                        } else if (u != null) {
                            if (qty == null) { qty = v; unit = u } else nameParts.add(tk)
                            i++; continue
                        }
                    }
                }
            }

            if (tk !in DROP) nameParts.add(tk)
            i++
        }

        var price = explicitPrice ?: 0L
        if (explicitPrice == null) {
            val big = bare.filter { it >= 500 }.maxOrNull()
            if (big != null) {
                price = Math.round(big)
                bare.remove(big)
            }
        }
        if (qty == null) {
            val small = bare.firstOrNull { it < 500 || explicitPrice != null }
            if (small != null) {
                qty = small
                unit = "pcs"
            }
        }
        val name = nameParts.joinToString(" ").trim().replaceFirstChar { it.uppercase() }
        return Parsed(name, qty ?: 1.0, unit, price, detectKind(name))
    }
}
