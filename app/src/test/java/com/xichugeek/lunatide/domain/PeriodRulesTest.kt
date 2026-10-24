package com.xichugeek.lunatide.domain

import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PeriodRulesTest {
    private val today = LocalDate.of(2026, 10, 24)
    private fun d(day: Int) = LocalDate.of(2026, 10, day)
    private fun p(start: Int, end: Int?, id: Long = start.toLong()) = Period(id, d(start), end?.let(::d))
    private fun apply(input: List<Period>, action: PeriodAction, day: Int, recent: Boolean = false, shorten: Boolean = false): PeriodEdit =
        PeriodRules.edit(input, action, d(day), today, recent, shorten)
    private fun rows(result: PeriodEdit): List<Period> = (result as PeriodEdit.Applied).periods.also {
        assertTrue(PeriodRules.valid(it, today))
    }
    private fun issue(result: PeriodEdit, expected: EditIssue) = assertEquals(PeriodEdit.Rejected(expected), result)

    @Test fun startCreatesOngoing() { assertNull(rows(apply(emptyList(), PeriodAction.START, 24)).single().end) }
    @Test fun startInsideNoOp() { issue(apply(listOf(p(1, 5)), PeriodAction.START, 3), EditIssue.ALREADY_INCLUDED) }
    @Test fun startRefusesPreviousOngoingBeyondEffectiveEnd() { issue(apply(listOf(p(1, null)), PeriodAction.START, 24), EditIssue.PREVIOUS_ONGOING) }
    @Test fun startExtendsLeft() { assertEquals(d(6), rows(apply(listOf(p(1, 5)), PeriodAction.START, 6)).single().end) }
    @Test fun startExtendsRight() { assertEquals(d(9), rows(apply(listOf(p(10, 14)), PeriodAction.START, 9)).single().start) }
    @Test fun historicalStartBeforeLaterIsClosed() { assertEquals(d(1), rows(apply(listOf(p(15, 19)), PeriodAction.START, 1)).first().end) }
    @Test fun endOngoing() { assertEquals(d(24), rows(apply(listOf(p(22, null)), PeriodAction.END, 24)).single().end) }
    @Test fun endWithoutOngoingFails() { issue(apply(listOf(p(1, 5)), PeriodAction.END, 24), EditIssue.NO_ONGOING) }
    @Test fun endBeforeStartFails() { issue(apply(listOf(p(22, null)), PeriodAction.END, 21), EditIssue.INVALID_END) }
    @Test fun futureRejectedForEveryAction() { PeriodAction.entries.forEach { issue(apply(emptyList(), it, 25), EditIssue.FUTURE_DATE) } }
    @Test fun bridgeMergesTwoPeriods() {
        val merged = rows(apply(listOf(p(1, 3), p(5, 8)), PeriodAction.INCLUDE, 4)).single()
        assertEquals(d(1), merged.start); assertEquals(d(8), merged.end)
    }
    @Test fun bridgeIntoOngoingPreservesOngoing() { assertNull(rows(apply(listOf(p(1, 3), p(5, null)), PeriodAction.INCLUDE, 4)).single().end) }
    @Test fun isolatedHistoricalInclusionIsSingleDay() { val added = rows(apply(emptyList(), PeriodAction.INCLUDE, 3)).single(); assertEquals(d(3), added.end) }
    @Test fun todayInclusionIsOngoing() { assertNull(rows(apply(emptyList(), PeriodAction.INCLUDE, 24)).single().end) }
    @Test fun includeNearPreviousRequiresConfirmation() { issue(apply(listOf(p(1, 5)), PeriodAction.INCLUDE, 10), EditIssue.CONFIRM_RECENT) }
    @Test fun startNearPreviousRequiresConfirmation() { issue(apply(listOf(p(1, 5)), PeriodAction.START, 10), EditIssue.CONFIRM_RECENT) }
    @Test fun confirmationAllowsNewPeriod() { assertEquals(2, rows(apply(listOf(p(1, 5)), PeriodAction.START, 10, recent = true)).size) }
    @Test fun tenDayGapDoesNotRequireConfirmation() { assertEquals(2, rows(apply(listOf(p(1, 5)), PeriodAction.START, 15)).size) }
    @Test fun removeSingleDay() { assertTrue(rows(apply(listOf(p(1, 1)), PeriodAction.EXCLUDE, 1)).isEmpty()) }
    @Test fun removeStartMovesStartOnly() {
        val result = apply(listOf(p(1, 5)), PeriodAction.EXCLUDE, 1) as PeriodEdit.Applied
        assertEquals(d(2), rows(result).single().start); assertEquals(DateRange(d(1), d(1)), result.clearFlow)
    }
    @Test fun removeEndShortens() { assertEquals(d(4), rows(apply(listOf(p(1, 5)), PeriodAction.EXCLUDE, 5)).single().end) }
    @Test fun removeTodayFromOngoingEndsYesterday() { assertEquals(d(23), rows(apply(listOf(p(22, null)), PeriodAction.EXCLUDE, 24)).single().end) }
    @Test fun middleRequiresConfirmation() { issue(apply(listOf(p(1, 5)), PeriodAction.EXCLUDE, 3), EditIssue.CONFIRM_SHORTEN) }
    @Test fun confirmedMiddleDoesNotSplit() {
        val result = apply(listOf(p(1, 5)), PeriodAction.EXCLUDE, 3, shorten = true) as PeriodEdit.Applied
        assertEquals(d(2), rows(result).single().end); assertEquals(DateRange(d(3), d(5)), result.clearFlow)
    }
    @Test fun excludeNonPeriodNoOp() { assertEquals(listOf(p(1, 5)), rows(apply(listOf(p(1, 5)), PeriodAction.EXCLUDE, 20))) }
    @Test fun ongoingEffectiveEndIsCappedAtFourteenDaysAfterStart() { assertEquals(d(15), p(1, null).effectiveEnd(today)) }
    @Test fun invalidOverlapAdjacentAndMultipleOngoingRejected() {
        assertFalse(PeriodRules.valid(listOf(p(1, 5), p(5, 8)), today))
        assertFalse(PeriodRules.valid(listOf(p(1, 5), p(6, 8)), today))
        assertFalse(PeriodRules.valid(listOf(p(1, null), p(20, null)), today))
        assertFalse(PeriodRules.valid(listOf(p(4, 3)), today))
    }
    @Test fun stableUnknownCodesAreIgnored() { assertNull(enumOrNull<Symptom>("FUTURE_SYMPTOM")); assertEquals(Mood.CALM, enumOrNull<Mood>("CALM")) }
    @Test fun dayLogValidation() {
        assertTrue(DayLog(today, bbtCentiCelsius = 3652, note = "a".repeat(500)).valid(today))
        assertFalse(DayLog(today, pain = 4).valid(today)); assertFalse(DayLog(today, bbtCentiCelsius = 4201).valid(today))
        assertFalse(DayLog(today.plusDays(1)).valid(today)); assertFalse(DayLog(today, note = "a".repeat(501)).valid(today))
    }
    @Test fun crossYearAndLeapDayWork() {
        val day = LocalDate.of(2028, 2, 29)
        assertTrue(PeriodRules.valid(rowsForDate(day), day))
        val old = Period(start = LocalDate.of(2026, 12, 31), end = null)
        val ended = PeriodRules.edit(listOf(old), PeriodAction.END, LocalDate.of(2027, 1, 2), LocalDate.of(2027, 1, 2))
        assertEquals(LocalDate.of(2027, 1, 2), (ended as PeriodEdit.Applied).periods.single().end)
    }
    private fun rowsForDate(day: LocalDate) = (PeriodRules.edit(emptyList(), PeriodAction.START, day, day) as PeriodEdit.Applied).periods
}
