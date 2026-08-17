package com.manageyourmoney.app.domain.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Direct port of the web app's `fmtINR()` / `fmtINRShort()` (index.html:503-520).
 * `Locale("en", "IN")` reproduces `toLocaleString('en-IN')`'s 2,2,3-digit grouping
 * (lakh/crore style, e.g. 12,34,567.00) which `Locale.US` would NOT give you.
 */
object CurrencyFormatter {

    private val enIn = Locale("en", "IN")

    private val fullFormat = DecimalFormat("##,##,##0.00", DecimalFormatSymbols(enIn))

    /** Mirrors `fmtINR(n)` — full currency presentation, always 2 decimal places. */
    fun full(amount: Double): String {
        val n = if (amount.isFinite()) amount else 0.0
        val neg = n < 0
        val formatted = fullFormat.format(abs(n))
        return (if (neg) "-" else "") + "\u20B9" + formatted
    }

    /** Mirrors `fmtINRShort(n)` — dynamic short scale: 1.5L, 2.5K, 1Cr, etc. */
    fun short(amount: Double): String {
        val n = if (amount.isFinite()) amount else 0.0
        val neg = n < 0
        val abs = abs(n)
        val (value, suffix) = when {
            abs >= 1e7 -> abs / 1e7 to "Cr"
            abs >= 1e5 -> abs / 1e5 to "L"
            abs >= 1e3 -> abs / 1e3 to "K"
            else -> abs to ""
        }
        val str = if (suffix.isNotEmpty()) {
            // toFixed(1) then strip a trailing ".0" — e.g. 1.50 -> "1.5", 2.00 -> "2"
            val oneDecimal = String.format(Locale.US, "%.1f", value)
            oneDecimal.removeSuffix(".0")
        } else {
            Math.round(value).toString()
        }
        return (if (neg) "-" else "") + "\u20B9" + str + suffix
    }
}
