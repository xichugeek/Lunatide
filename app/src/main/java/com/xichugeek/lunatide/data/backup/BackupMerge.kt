package com.xichugeek.lunatide.data.backup

import com.xichugeek.lunatide.domain.model.*
import java.time.LocalDate

data class ImportResult(val addedPeriods: Int, val addedDays: Int, val updatedDays: Int)
data class MergedRecords(val periods: List<Period>, val logs: List<DayLog>, val result: ImportResult)

/** Newest equal-start revision wins; remaining overlapping or adjacent spans form a union. */
fun mergeRecords(oldPeriods: List<Period>, oldLogs: List<DayLog>, incomingPeriods: List<Period>, incomingLogs: List<DayLog>, today: LocalDate): MergedRecords {
    val unique = (oldPeriods + incomingPeriods).groupBy { it.start }.values.map { same -> same.maxBy { it.updatedAt } }.sortedBy { it.start }
    val periods = mutableListOf<Period>()
    unique.forEach { next ->
        val previous = periods.lastOrNull()
        val previousEnd = previous?.end ?: today
        if (previous != null && next.start <= previousEnd.plusDays(1)) {
            periods[periods.lastIndex] = previous.copy(id = 0, end = if (previous.end == null || next.end == null) null else maxOf(previous.end, next.end), updatedAt = maxOf(previous.updatedAt, next.updatedAt))
        } else periods += next.copy(id = 0)
    }
    val oldByDay = oldLogs.associateBy { it.day }
    val newestIncoming = incomingLogs.groupBy { it.day }.values.map { it.maxBy(DayLog::updatedAt) }
    val logs = (oldLogs + newestIncoming).groupBy { it.day }.values.map { it.maxBy(DayLog::updatedAt) }.sortedBy { it.day }.map { log ->
        if (log.flow != null && log.flow != Flow.SPOTTING && periods.none { it.contains(log.day, today) }) log.copy(flow = null) else log
    }.filter { !it.isEmpty || periods.any { p -> p.contains(it.day, today) } }
    return MergedRecords(periods, logs, ImportResult(periods.count { p -> oldPeriods.none { it.start == p.start } },
        newestIncoming.count { it.day !in oldByDay }, newestIncoming.count { log -> oldByDay[log.day]?.let { log.updatedAt > it.updatedAt } == true }))
}
