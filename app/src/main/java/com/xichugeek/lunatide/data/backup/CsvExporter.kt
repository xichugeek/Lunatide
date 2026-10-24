package com.xichugeek.lunatide.data.backup

import com.xichugeek.lunatide.domain.model.*
import java.time.LocalDate
import java.util.Locale

data class CsvLabels(val headers: List<String>, val yes: String, val no: String, val flows: Map<Flow, String>, val pain: Map<Int, String>,
    val symptoms: Map<Symptom, String>, val moods: Map<Mood, String>, val intercourse: Map<Intercourse, String>)

/** A single RFC 4180 CSV with UTF-8 BOM and literal, non-executable spreadsheet text. */
fun exportCsv(periods: List<Period>, logs: List<DayLog>, today: LocalDate, showIntercourse: Boolean, labels: CsvLabels): ByteArray {
    val days = logs.map { it.day }.toSortedSet()
    periods.forEach { period ->
        val end = period.effectiveEnd(today)
        if (end.toEpochDay() - period.start.toEpochDay() > 100_000) throw BackupException(BackupError.TOO_LARGE)
        var day = period.start
        while (day <= end) { days += day; day = day.plusDays(1) }
    }
    if (days.size > 100_000) throw BackupException(BackupError.TOO_LARGE)
    val byDay = logs.associateBy { it.day }
    fun cell(value: String): String {
        val safe = if (value.trimStart().firstOrNull() in setOf('=', '+', '-', '@', '\t', '\r')) "'" + value else value
        return if (safe.any { it in ",\"\r\n" }) "\"${safe.replace("\"", "\"\"")}\"" else safe
    }
    fun row(values: List<String>) = values.joinToString(",", transform = ::cell) + "\r\n"
    val headers = if (showIntercourse) labels.headers else labels.headers.filterIndexed { index, _ -> index != 8 }
    val text = buildString {
        append('\uFEFF'); append(row(headers))
        days.forEach { day ->
            val log = byDay[day]
            val period = periods.firstOrNull { it.contains(day, today) }
            val values = listOf(day.toString(), if (period != null) labels.yes else labels.no,
                period?.let { (day.toEpochDay() - it.start.toEpochDay() + 1).toString() }.orEmpty(),
                log?.flow?.let { labels.flows[it] }.orEmpty(), log?.pain?.let { labels.pain[it] }.orEmpty(),
                log?.symptoms?.sortedBy { it.ordinal }?.joinToString("; ") { labels.symptoms[it].orEmpty() }.orEmpty(),
                log?.mood?.let { labels.moods[it] }.orEmpty(), log?.bbtCentiCelsius?.let { String.format(Locale.ROOT, "%.2f", it / 100.0) }.orEmpty(),
                log?.intercourse?.let { labels.intercourse[it] }.orEmpty(), log?.note.orEmpty())
            append(row(if (showIntercourse) values else values.filterIndexed { index, _ -> index != 8 }))
        }
    }
    return text.toByteArray(Charsets.UTF_8)
}
