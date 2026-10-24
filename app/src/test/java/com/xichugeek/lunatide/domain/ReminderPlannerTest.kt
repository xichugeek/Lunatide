package com.xichugeek.lunatide.domain

import com.xichugeek.lunatide.domain.model.Period
import com.xichugeek.lunatide.domain.prediction.PredictionConfig
import com.xichugeek.lunatide.domain.reminder.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderPlannerTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 10, 24)
    private val history = listOf(Period(start = LocalDate.of(2026, 9, 28), end = LocalDate.of(2026, 10, 2)))
    private fun config(vararg kinds: ReminderKind) = ReminderConfig(kinds.toSet())
    private fun now(day: LocalDate = today, hour: Int = 19, minute: Int = 0) = day.atTime(hour, minute).atZone(zone)

    @Test fun upcomingUsesConfiguredLeadDaysAndTime() {
        val planned = planReminders(history, now(), config(ReminderKind.UPCOMING)).single()
        assertEquals(today.atTime(20, 0).atZone(zone), planned.at)
        assertEquals(setOf(ReminderKind.UPCOMING), dueReminders(history, today, config(ReminderKind.UPCOMING)))
    }
    @Test fun startOnlyOnOriginalPredictedDate() {
        val date = LocalDate.of(2026, 10, 26)
        assertEquals(date, planReminders(history, now(), config(ReminderKind.START)).single().at.toLocalDate())
        assertTrue(ReminderKind.START in dueReminders(history, date, config(ReminderKind.START)))
        assertTrue(dueReminders(history, date.plusDays(1), config(ReminderKind.START)).isEmpty())
    }
    @Test fun newRecordedPeriodSuppressesStaleStartReminder() {
        val recorded = history + Period(start = LocalDate.of(2026, 10, 26), end = null)
        assertFalse(ReminderKind.START in dueReminders(recorded, LocalDate.of(2026, 10, 26), config(ReminderKind.START)))
    }
    @Test fun endAtAveragePlusTwoDaysAndRepeatsUntilUpdated() {
        val active = listOf(Period(start = today.minusDays(6), end = null))
        assertTrue(ReminderKind.END in dueReminders(active, today, config(ReminderKind.END)))
        assertFalse(ReminderKind.END in dueReminders(active, today.minusDays(1), config(ReminderKind.END)))
        assertEquals(today.plusDays(1), planReminders(active, now(hour = 21), config(ReminderKind.END)).single().at.toLocalDate())
    }
    @Test fun completedPeriodDoesNotGetEndReminder() {
        val finished = listOf(Period(start = today.minusDays(8), end = today.minusDays(3)))
        assertTrue(dueReminders(finished, today, config(ReminderKind.END)).isEmpty())
    }
    @Test fun dailyWorksWithoutHealthHistory() {
        assertEquals(today, planReminders(emptyList(), now(), config(ReminderKind.DAILY)).single().at.toLocalDate())
        assertEquals(today.plusDays(1), planReminders(emptyList(), now(hour = 21), config(ReminderKind.DAILY)).single().at.toLocalDate())
    }
    @Test fun noRetroactiveUpcomingSchedulingAfterReminderTime() { assertTrue(planReminders(history, now(hour = 21), config(ReminderKind.UPCOMING)).isEmpty()) }
    @Test fun dueJobIsPreservedDuringColdStartAfterScheduledTime() {
        val current = now(hour = 20, minute = 1).toInstant()
        val scheduled = now(hour = 20).toInstant()
        assertTrue(preserveDueJob(ReminderKind.UPCOMING, setOf(ReminderKind.UPCOMING), scheduled, current, true))
        assertFalse(preserveDueJob(ReminderKind.UPCOMING, emptySet(), scheduled, current, true))
        assertFalse(preserveDueJob(ReminderKind.UPCOMING, setOf(ReminderKind.UPCOMING), scheduled, current, false))
    }
    @Test fun fertileReminderOnlyFirstDayNotEachClippedDay() {
        val p = listOf(Period(start = LocalDate.of(2026, 10, 4), end = LocalDate.of(2026, 10, 8)))
        val c = config(ReminderKind.FERTILE)
        assertTrue(ReminderKind.FERTILE in dueReminders(p, LocalDate.of(2026, 10, 13), c))
        assertFalse(ReminderKind.FERTILE in dueReminders(p, LocalDate.of(2026, 10, 14), c))
    }
    @Test fun fertileFirstDayAfterTrimmingActualPeriod() {
        val p = listOf(Period(start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 7)))
        val c = config(ReminderKind.FERTILE).copy(prediction = PredictionConfig(manualCycleLength = 21, manualPeriodLength = 7))
        assertTrue(ReminderKind.FERTILE in dueReminders(p, LocalDate.of(2026, 10, 8), c))
    }
    @Test fun hiddenFertilityNeverSchedulesFertileReminder() {
        val c = config(ReminderKind.FERTILE).copy(prediction = PredictionConfig(showFertility = false))
        assertTrue(planReminders(history, now(), c).isEmpty())
    }
    @Test fun disabledRemindersHaveNoPlans() { assertTrue(planReminders(history, now(), config()).isEmpty()); assertTrue(dueReminders(history, today, config()).isEmpty()) }
    @Test fun daylightSavingUsesZonedTime() {
        val current = ZonedDateTime.of(2026, 3, 7, 21, 0, 0, 0, ZoneId.of("America/New_York"))
        val target = planReminders(emptyList(), current, config(ReminderKind.DAILY)).single().at
        assertEquals(20, target.hour); assertEquals(22L, Duration.between(current.toInstant(), target.toInstant()).toHours())
    }
    @Test fun crossYearDailyScheduling() {
        val current = ZonedDateTime.of(2026, 12, 31, 23, 0, 0, 0, zone)
        assertEquals(LocalDate.of(2027, 1, 1), planReminders(emptyList(), current, config(ReminderKind.DAILY)).single().at.toLocalDate())
    }
}
