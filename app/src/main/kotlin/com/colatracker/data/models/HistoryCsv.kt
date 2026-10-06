package com.colatracker.data.models

/**
 * Экспорт истории в CSV для Excel/Google Sheets.
 *
 * Разделитель `;` и BOM в начале: российский Excel без них открывает файл одной колонкой
 * и ломает кириллицу. Строки — как в приложении: новые записи сверху.
 */
const val CSV_BOM = "﻿"

private const val CSV_SEPARATOR = ";"

fun historyToCsv(childName: String, history: List<DrinkHistoryItem>): String {
    val sb = StringBuilder(CSV_BOM)
    sb.append(listOf("Дата", "Время", "Объём (мл)", "Ребёнок").joinToString(CSV_SEPARATOR)).append("\r\n")
    history.forEach { item ->
        sb.append(
            listOf(
                csvCell(item.getFormattedDate()),
                csvCell(item.getFormattedTime()),
                item.amountMl.toString(),
                csvCell(childName)
            ).joinToString(CSV_SEPARATOR)
        ).append("\r\n")
    }
    return sb.toString()
}

/**
 * Ячейка по RFC 4180: значения с разделителем, кавычками или переводом строки берутся в кавычки,
 * внутренние кавычки удваиваются. Значения, которые Excel исполнил бы как формулу (`=`, `+`, `-`, `@`),
 * получают апостроф в начале — имя ребёнка приходит с сервера и не должно ничего выполнять.
 */
internal fun csvCell(value: String): String {
    val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
    val needsQuotes = safe.any { it == ';' || it == ',' || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
}

/** Безопасное имя файла: `cola-Артем-2026-10-06.csv`. */
fun csvFileName(childName: String, dateIso: String): String {
    val cleaned = childName.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        .ifEmpty { "ребенок" }
        .take(40)
    return "cola-$cleaned-$dateIso.csv"
}
