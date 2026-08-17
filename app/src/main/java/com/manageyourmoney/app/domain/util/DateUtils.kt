package com.manageyourmoney.app.domain.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Direct ports of index.html:559-582. A "month key" is always the "yyyy-MM" string the
 * web app used as both the Store key suffix (`month:<key>`) and the sort key for
 * `State.monthsIndex`.
 */
object DateUtils {

    private val enIn = Locale("en", "IN")
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Mirrors `todayStr()`. */
    fun todayStr(): String = LocalDate.now().format(isoDate)

    /** Mirrors `currentMonthKey()`. */
    fun currentMonthKey(): String {
        val now = YearMonth.now()
        return "%04d-%02d".format(now.year, now.monthValue)
    }

    /** Mirrors `monthKeyLabel(key)` — e.g. "August 2026". */
    fun monthKeyLabel(key: String): String {
        val ym = parseMonthKey(key)
        val monthName = ym.month.getDisplayName(TextStyle.FULL, enIn)
        return "$monthName ${ym.year}"
    }

    /** Mirrors `monthKeyShort(key)` — e.g. "Aug 26". */
    fun monthKeyShort(key: String): String {
        val ym = parseMonthKey(key)
        val monthName = ym.month.getDisplayName(TextStyle.SHORT, enIn)
        val yy = "%02d".format(ym.year % 100)
        return "$monthName $yy"
    }

    /** Mirrors `addMonths(key, n)`. */
    fun addMonths(key: String, n: Int): String {
        val ym = parseMonthKey(key).plusMonths(n.toLong())
        return "%04d-%02d".format(ym.year, ym.monthValue)
    }

    /** Mirrors `diffMonths(fromKey, toKey)` — signed month count, toKey minus fromKey. */
    fun diffMonths(fromKey: String, toKey: String): Int {
        val from = parseMonthKey(fromKey)
        val to = parseMonthKey(toKey)
        return (to.year - from.year) * 12 + (to.monthValue - from.monthValue)
    }

    /** yyyy-MM-01, used for the synthetic date on generated EMI rows (matches `monthKey+'-01'`). */
    fun monthKeyToFirstOfMonth(key: String): String = "$key-01"

    fun parseMonthKey(key: String): YearMonth {
        val (y, m) = key.split("-").map { it.toInt() }
        return YearMonth.of(y, m)
    }

    fun parseDate(date: String): LocalDate = LocalDate.parse(date, isoDate)
}
