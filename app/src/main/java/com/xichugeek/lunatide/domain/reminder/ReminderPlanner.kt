package com.xichugeek.lunatide.domain.reminder

import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.prediction.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class ReminderKind { UPCOMING, START, END, DAILY, FERTILE }
data class ReminderConfig(
    val enabled: Set<ReminderKind>, val timeMinutes: Int = 1200, val daysBefore: Int = 2,
    val prediction: PredictionConfig = PredictionConfig(),
)
data class PlannedReminder(val kind: ReminderKind, val at: ZonedDateTime)

/** All scheduling decisions are pure; delayed workers must re-evaluate the same conditions. */
fun dueReminders(periods: List<Period>, today: LocalDate, config: ReminderConfig): Set<ReminderKind> {
    val history = periods.filter { it.start <= today }
    val p = predict(history, today, config.prediction)
    val ongoing = history.lastOrNull { it.end == null }
    return buildSet {
        if (ReminderKind.DAILY in config.enabled) add(ReminderKind.DAILY)
        if (ongoing == null) {
            if (p.nextPeriodStart?.minusDays(config.daysBefore.toLong()) == today && ReminderKind.UPCOMING in config.enabled) add(ReminderKind.UPCOMING)
            if (p.nextPeriodStart == today && ReminderKind.START in config.enabled) add(ReminderKind.START)
            if (today in fertileFirstDays(history, today, p, config) && ReminderKind.FERTILE in config.enabled) add(ReminderKind.FERTILE)
        } else if (today.toEpochDay() - ongoing.start.toEpochDay() + 1 >= p.periodLength + 2 && ReminderKind.END in config.enabled) add(ReminderKind.END)
    }
}

fun planReminders(periods: List<Period>, now: ZonedDateTime, config: ReminderConfig): List<PlannedReminder> {
    val today = now.toLocalDate()
    val p = predict(periods, today, config.prediction)
    val time = LocalTime.of(config.timeMinutes.coerceIn(0, 1439) / 60, config.timeMinutes.coerceIn(0, 1439) % 60)
    val ongoing = periods.lastOrNull { it.start <= today && it.end == null }
    fun at(day: LocalDate) = day.atTime(time).atZone(now.zone)
    return config.enabled.mapNotNull { kind ->
        val candidate = when (kind) {
            ReminderKind.UPCOMING -> p.nextPeriodStart?.minusDays(config.daysBefore.toLong())
            ReminderKind.START -> p.nextPeriodStart
            ReminderKind.FERTILE -> fertileFirstDays(periods, today, p, config).firstOrNull { at(it) > now }
            ReminderKind.END -> ongoing?.start?.plusDays(p.periodLength + 1L)?.let { maxOf(it, today) }
            ReminderKind.DAILY -> today
        } ?: return@mapNotNull null
        var target = at(candidate)
        if (target <= now && kind in setOf(ReminderKind.END, ReminderKind.DAILY)) target = at(today.plusDays(1))
        if (target <= now) null else PlannedReminder(kind, target)
    }
}

private fun fertileFirstDays(periods: List<Period>, today: LocalDate, prediction: PredictionResult, config: ReminderConfig): List<LocalDate> {
    if (prediction.fertilityHiddenReason != null) return emptyList()
    val blocked = periods.filter { it.start <= today }.map { DateRange(it.start, it.effectiveEnd(today)) } + prediction.predictedPeriods
    return (listOfNotNull(prediction.nextPeriodStart) + prediction.predictedPeriods.map { it.start }).distinct().mapNotNull { start ->
        val from = start.minusDays(config.prediction.lutealPhaseDays + 5L)
        (0L..6L).map { from.plusDays(it) }.firstOrNull { day -> blocked.none { day in it } }
    }.distinct().sorted()
}

fun preserveDueJob(kind: ReminderKind, due: Set<ReminderKind>, scheduled: Instant, now: Instant, sameTimeConfiguration: Boolean): Boolean =
    kind in due && scheduled <= now && sameTimeConfiguration
