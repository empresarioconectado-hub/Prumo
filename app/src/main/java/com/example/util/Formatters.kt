package com.example.util

import java.text.NumberFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

private val ptBrLocale = Locale.Builder().setLanguage("pt").setRegion("BR").build()

fun formatBrlFromCents(cents: Long, includeDecimals: Boolean = true): String {
    val format = NumberFormat.getCurrencyInstance(ptBrLocale).apply {
        maximumFractionDigits = if (includeDecimals) 2 else 0
        minimumFractionDigits = if (includeDecimals) 2 else 0
    }
    return format.format(cents / 100.0)
}

fun formatBrlCompact(cents: Long): String {
    val absReais = kotlin.math.abs(cents) / 100.0
    val sign = if (cents < 0) "-" else ""
    return when {
        absReais >= 1_000_000.0 -> "${sign}R$ ${String.format(ptBrLocale, "%.1fM", absReais / 1_000_000.0)}"
        absReais >= 1_000.0 -> "${sign}R$ ${String.format(ptBrLocale, "%.1fk", absReais / 1_000.0)}"
        else -> formatBrlFromCents(cents, includeDecimals = false)
    }
}

fun parseBrlInputToCents(raw: String): Long? {
    val trimmed = raw.trim().replace("R$", "").replace(" ", "")
    if (trimmed.isEmpty()) return null
    val normalized = when {
        trimmed.contains(",") && trimmed.contains(".") ->
            trimmed.replace(".", "").replace(",", ".")
        trimmed.contains(",") ->
            trimmed.replace(",", ".")
        else -> trimmed
    }
    val value = normalized.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return (value * 100.0).roundToLong()
}

fun centsToInputText(cents: Long): String {
    val reais = cents / 100.0
    return String.format(ptBrLocale, "%.2f", reais)
}

fun formatDateShort(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
    return sdf.format(Date(millis))
}

fun formatDayMonth(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM", ptBrLocale)
    return sdf.format(Date(millis))
}

fun formatPercent(value: Double): String {
    return String.format(ptBrLocale, "%.1f%%", value)
}
