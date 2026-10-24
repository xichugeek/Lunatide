package com.xichugeek.lunatide.domain

import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.prediction.*
import com.xichugeek.lunatide.domain.stats.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PredictionTest {
    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun history(vararg lengths: Int): List<Period> {
        var start = LocalDate.of(2025, 1, 1)
        return buildList {
            add(Period(start = start, end = start.plusDays(4)))
            lengths.forEach { start = start.plusDays(it.toLong()); add(Period(start = start, end = start.plusDays(4))) }
        }
    }
    private fun result(vararg lengths: Int, config: PredictionConfig = PredictionConfig()): PredictionResult {
        val p = history(*lengths); return predict(p, p.last().start.plusDays(8), config)
    }
    @Test fun t01NoRecords() {
        val r = predict(emptyList(), d(10, 1)); assertNull(r.nextPeriodStart); assertNull(r.cycleDay)
        assertTrue(r.predictedPeriods.isEmpty()); assertEquals(Confidence.NONE, r.confidence)
        assertEquals(FertilityHiddenReason.NOT_ENOUGH_DATA, r.fertilityHiddenReason)
    }
    @Test fun t02OnePeriodDefaults() {
        val r = predict(listOf(Period(start = d(10, 1), end = d(10, 5))), d(10, 8))
        assertEquals(28, r.cycleLength); assertEquals(5, r.periodLength); assertEquals(d(10, 29), r.nextPeriodStart); assertEquals(Confidence.LOW, r.confidence)
    }
    @Test fun t03InitialCycle() { assertEquals(32, result(config = PredictionConfig(initialCycleLength = 32)).cycleLength) }
    @Test fun t04PriorBlending() { assertEquals(29, result(30).cycleLength) }
    @Test fun t05RegularCycles() {
        val r = result(28, 28, 28, 28, 28, 28); assertEquals(28, r.cycleLength); assertEquals(1, r.rangeDays); assertEquals(Confidence.HIGH, r.confidence)
        assertEquals(Regularity.REGULAR, computeStats(history(28, 28, 28, 28, 28, 28), emptyList(), d(10, 24)).regularity)
    }
    @Test fun t06WeightedMean() { assertEquals(29, result(26, 27, 28, 29, 30, 31).cycleLength) }
    @Test fun t07MissedCycleOutlier() { assertEquals(listOf(28, 29, 28, 30), validCycleLengths(history(28, 29, 56, 28, 30))); assertEquals(29, result(28, 29, 56, 28, 30).cycleLength) }
    @Test fun t08HardFilter() { assertEquals(listOf(28, 29), validCycleLengths(history(10, 28, 100, 29))) }
    @Test fun t09ManualCycle() { assertEquals(35, result(28, 28, 28, config = PredictionConfig(manualCycleLength = 35)).cycleLength) }
    @Test fun t10OngoingDayThree() {
        val r = predict(listOf(Period(start = d(10, 1), end = null)), d(10, 3)); assertTrue(r.inPeriod); assertEquals(3, r.cycleDay); assertEquals(0, r.daysLate)
    }
    @Test fun t11LongOngoingCapped() {
        val period = Period(start = d(10, 1), end = null); assertEquals(d(10, 15), period.effectiveEnd(d(10, 20)))
        assertFalse(predict(listOf(period), d(10, 20)).inPeriod)
    }
    @Test fun t12LateKeepsOriginalAndMovesCalendarAnchor() {
        val r = predict(listOf(Period(start = d(10, 1), end = d(10, 5))), d(11, 2))
        assertEquals(4, r.daysLate); assertEquals(d(10, 29), r.nextPeriodStart); assertEquals(d(11, 2), r.predictedPeriods.first().start)
    }
    @Test fun t13DueToday() { assertEquals(0, predict(listOf(Period(start = d(10, 1), end = d(10, 5))), d(10, 29)).daysLate) }
    @Test fun t14Fertility() {
        val r = predict(listOf(Period(start = d(10, 4), end = d(10, 8))), d(10, 10))
        assertEquals(d(11, 1), r.nextPeriodStart); assertEquals(d(10, 18), r.ovulationDays.first()); assertEquals(DateRange(d(10, 13), d(10, 19)), r.fertileWindows.first())
    }
    @Test fun t15OutOfRange() { val r = result(config = PredictionConfig(manualCycleLength = 40)); assertTrue(r.fertileWindows.isEmpty()); assertEquals(FertilityHiddenReason.OUT_OF_RANGE, r.fertilityHiddenReason) }
    @Test fun t16Irregular() {
        val periods = history(21, 21, 21, 38, 38, 38); val today = periods.last().start.plusDays(8)
        assertEquals(Regularity.IRREGULAR, computeStats(periods, emptyList(), today).regularity)
        assertEquals(FertilityHiddenReason.IRREGULAR, predict(periods, today).fertilityHiddenReason)
    }
    @Test fun t17Disabled() { assertEquals(FertilityHiddenReason.DISABLED, result(config = PredictionConfig(showFertility = false)).fertilityHiddenReason) }
    @Test fun t18CrossYear() {
        val start = LocalDate.of(2026, 12, 20); assertEquals(LocalDate.of(2027, 1, 17), predict(listOf(Period(start = start, end = null)), start).nextPeriodStart)
    }
    @Test fun t19LeapYear() {
        val start = LocalDate.of(2028, 2, 10); assertEquals(LocalDate.of(2028, 3, 9), predict(listOf(Period(start = start, end = null)), start).nextPeriodStart)
    }
    @Test fun t20SixForecastsDoNotOverlapCurrent() {
        val start = d(10, 1); val r = predict(listOf(Period(start = start, end = null)), d(10, 3))
        assertEquals(6, r.predictedPeriods.size); r.predictedPeriods.zipWithNext().forEach { (a, b) -> assertEquals(28L, b.start.toEpochDay() - a.start.toEpochDay()) }
        assertTrue(r.predictedPeriods.first().start > start.plusDays(14))
    }
    @Test fun t21TrimFertilityOverlaps() {
        val p = Period(start = d(10, 1), end = d(10, 7)); val r = predict(listOf(p), d(10, 1), PredictionConfig(manualCycleLength = 21, manualPeriodLength = 7))
        r.fertileWindows.forEach { f ->
            var day = f.start
            while (day <= f.end) { assertFalse(p.contains(day, d(10, 1))); assertTrue(r.predictedPeriods.none { day in it }); day = day.plusDays(1) }
        }
        assertEquals(d(10, 8), r.fertileWindows.first().start)
    }
    @Test fun t22PeriodAverageExcludesOngoing() {
        val p = listOf(Period(start = d(7, 1), end = d(7, 5)), Period(start = d(8, 1), end = d(8, 6)), Period(start = d(9, 1), end = d(9, 7)), Period(start = d(10, 1), end = null))
        assertEquals(6, predict(p, d(10, 3)).periodLength)
    }
    @Test fun t23TopSymptomsCountOrderAndLimit() {
        val p = history(28, 28, 28); val date = p.last().start
        val logs = listOf(DayLog(date, symptoms = Symptom.entries.toSet()), DayLog(date.plusDays(1), symptoms = setOf(Symptom.FATIGUE, Symptom.CRAMPS)))
        val result = computeStats(p, logs, date.plusDays(2))
        assertEquals(5, result.topSymptoms.size); assertEquals(Symptom.CRAMPS to 2, result.topSymptoms[0]); assertEquals(Symptom.FATIGUE to 2, result.topSymptoms[1])
    }
    @Test fun t24LongCyclesTip() { val p = history(38, 38, 38); assertTrue(GentleTip.CYCLE_OUT_OF_COMMON_RANGE in computeStats(p, emptyList(), p.last().start).gentleTips) }
    @Test fun t25NoTipsWithOnlyTwoCycles() { val p = history(38, 38); assertTrue(computeStats(p, emptyList(), p.last().start).gentleTips.isEmpty()) }
    @Test fun futureHistoryIgnoredWhenSystemDateMovesBackwards() {
        val r = predict(listOf(Period(start = d(10, 20), end = d(10, 23))), d(10, 10)); assertNull(r.nextPeriodStart); assertEquals(Confidence.NONE, r.confidence)
    }
    @Test fun manualInputsAreClamped() { val r = result(config = PredictionConfig(manualCycleLength = 200, manualPeriodLength = 0)); assertEquals(90, r.cycleLength); assertEquals(2, r.periodLength) }
    @Test fun populationDeviationAndMediumConfidence() { assertEquals(Confidence.MEDIUM, result(24, 28, 32).confidence); assertEquals(3, result(24, 28, 32).rangeDays) }
    @Test fun painAndLongPeriodTipsRequireEnoughHistory() {
        val p = history(28, 28, 28).map { it.copy(end = it.start.plusDays(8)) }
        val logs = p.takeLast(2).map { DayLog(it.start, pain = 3) }
        val stats = computeStats(p, logs, p.last().start.plusDays(9))
        assertTrue(GentleTip.LONG_PERIODS in stats.gentleTips); assertTrue(GentleTip.SEVERE_PAIN in stats.gentleTips); assertEquals(2, stats.painDistribution[3])
    }
    @Test fun forecastsAndFertilityNeverShowPastDates() {
        val r = predict(listOf(Period(start = d(10, 1), end = d(10, 5))), d(12, 20))
        assertTrue(r.predictedPeriods.all { it.start >= d(12, 20) }); assertTrue(r.fertileWindows.all { it.start >= d(12, 20) })
    }
    @Test fun twentyYearsOfSyntheticRecords() {
        val base = LocalDate.of(2006, 1, 1)
        val periods = (0 until 260).map { index -> val start = base.plusDays(index * 28L); Period(start = start, end = start.plusDays(4)) }
        val logs = (0 until 7300).map { DayLog(base.plusDays(it.toLong()), symptoms = setOf(Symptom.FATIGUE)) }
        val today = base.plusDays(7300)
        repeat(20) { predict(periods, today) }
        val elapsed = kotlin.system.measureNanoTime { repeat(100) { predict(periods, today) } } / 100.0 / 1_000_000
        assertEquals(6, predict(periods, today).predictedPeriods.size)
        assertEquals(260, computeStats(periods, logs, today).cycles.size)
        System.out.printf("Synthetic 20-year prediction mean: %.3f ms%n", elapsed)
    }
}
