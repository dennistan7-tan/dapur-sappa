package com.dapursappa.app.data

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.Locale

object Fmt {
    val ID: Locale = Locale.forLanguageTag("id-ID")
    private val dLong = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", ID)
    private val dDay = DateTimeFormatter.ofPattern("EEEE", ID)
    private val dMon = DateTimeFormatter.ofPattern("d MMM", ID)
    private val dShortDay = DateTimeFormatter.ofPattern("EEE, d MMM", ID)
    private val dMonth = DateTimeFormatter.ofPattern("MMMM yyyy", ID)
    private val dRange = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val tFmt = DateTimeFormatter.ofPattern("HH.mm", ID)

    fun rp(v: Long): String {
        val s = NumberFormat.getIntegerInstance(ID).format(Math.abs(v))
        return (if (v < 0) "-Rp" else "Rp") + s
    }

    fun num(d: Double): String {
        if (d == Math.floor(d) && Math.abs(d) < 1e12) return d.toLong().toString()
        return String.format(ID, "%.2f", d).trimEnd('0').trimEnd(',')
    }

    fun thousands(v: Long): String = NumberFormat.getIntegerInstance(ID).format(v)

    fun pct(profit: Long, rev: Long): String = if (rev > 0) String.format(ID, "%.1f%%", profit * 100.0 / rev) else "-"

    fun dayLong(d: LocalDate): String = d.format(dLong)
    fun dayName(d: LocalDate): String = d.format(dDay)
    fun dayMon(d: LocalDate): String = d.format(dMon)
    fun dayShort(d: LocalDate): String = d.format(dShortDay)
    fun monthName(d: LocalDate): String = d.format(dMonth)
    fun range(a: LocalDate, b: LocalDate): String = a.format(dRange) + " – " + b.format(dRange)
    fun time(ts: Long): String = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(tFmt)
    fun dateOf(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

    fun mondayOf(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun weekNo(d: LocalDate): Int = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
}
