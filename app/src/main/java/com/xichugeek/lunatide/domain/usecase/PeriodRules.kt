package com.xichugeek.lunatide.domain.usecase

import com.xichugeek.lunatide.domain.model.*
import java.time.LocalDate

enum class PeriodAction { START, END, INCLUDE, EXCLUDE }
enum class EditIssue { FUTURE_DATE, ALREADY_INCLUDED, PREVIOUS_ONGOING, NO_ONGOING, INVALID_END, CONFIRM_RECENT, CONFIRM_SHORTEN }
sealed interface PeriodEdit {
    data class Applied(val periods: List<Period>, val clearFlow: DateRange? = null) : PeriodEdit
    data class Rejected(val issue: EditIssue) : PeriodEdit
}

/** Pure period editing rules. The repository commits this result and flow cleanup atomically. */
object PeriodRules {
    fun edit(
        input: List<Period>, action: PeriodAction, day: LocalDate, today: LocalDate,
        confirmRecent: Boolean = false, confirmShorten: Boolean = false,
    ): PeriodEdit {
        if (day > today) return PeriodEdit.Rejected(EditIssue.FUTURE_DATE)
        val periods = input.sortedBy { it.start }
        val containing = periods.firstOrNull { it.contains(day, today) }
        val ongoing = periods.firstOrNull { it.end == null }
        if (action == PeriodAction.END) {
            if (ongoing == null) return PeriodEdit.Rejected(EditIssue.NO_ONGOING)
            if (day < ongoing.start) return PeriodEdit.Rejected(EditIssue.INVALID_END)
            return PeriodEdit.Applied(periods.map { if (it == ongoing) it.copy(end = day) else it })
        }
        if (action == PeriodAction.EXCLUDE) {
            if (containing == null) return PeriodEdit.Applied(periods)
            val last = containing.effectiveEnd(today)
            val replacement = when {
                containing.start == day && last == day -> null
                containing.start == day -> containing.copy(start = day.plusDays(1))
                containing.end == day || (containing.end == null && day == today) -> containing.copy(end = day.minusDays(1))
                !confirmShorten -> return PeriodEdit.Rejected(EditIssue.CONFIRM_SHORTEN)
                else -> containing.copy(end = day.minusDays(1))
            }
            val changed = periods.filterNot { it == containing } + listOfNotNull(replacement)
            val removedEnd = if (day == containing.start) day else last
            return PeriodEdit.Applied(changed.sortedBy { it.start }, DateRange(day, removedEnd))
        }
        if (containing != null) return PeriodEdit.Rejected(EditIssue.ALREADY_INCLUDED)
        if (ongoing != null && day > ongoing.start) return PeriodEdit.Rejected(EditIssue.PREVIOUS_ONGOING)
        val left = periods.firstOrNull { it.end?.plusDays(1) == day }
        val right = periods.firstOrNull { it.start.minusDays(1) == day }
        if (left != null || right != null) {
            val merged = when {
                left != null && right != null -> left.copy(end = right.end)
                left != null -> left.copy(end = day)
                right != null -> right.copy(start = day)
                else -> error("Unreachable")
            }
            return PeriodEdit.Applied((periods.filterNot { it == left || it == right } + merged).sortedBy { it.start })
        }
        val previousEnd = periods.lastOrNull { it.start < day }?.end
        if (!confirmRecent && previousEnd != null && day.toEpochDay() - previousEnd.toEpochDay() < 10) {
            return PeriodEdit.Rejected(EditIssue.CONFIRM_RECENT)
        }
        val latest = periods.none { it.start > day }
        val end = if (latest && (action == PeriodAction.START || day == today)) null else day
        return PeriodEdit.Applied((periods + Period(start = day, end = end)).sortedBy { it.start })
    }

    fun valid(periods: List<Period>, today: LocalDate): Boolean {
        val sorted = periods.sortedBy { it.start }
        return sorted.all { it.start <= today && (it.end == null || it.end in it.start..today) } &&
            sorted.count { it.end == null } <= 1 && sorted.dropLast(1).none { it.end == null } &&
            sorted.zipWithNext().all { (a, b) -> a.end?.let { it.plusDays(1) < b.start } ?: false }
    }
}
