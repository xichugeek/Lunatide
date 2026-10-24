package com.xichugeek.lunatide.domain.prediction

import com.xichugeek.lunatide.domain.model.*
import java.time.LocalDate
import kotlin.math.*

enum class Confidence { NONE, LOW, MEDIUM, HIGH }
enum class FertilityHiddenReason { DISABLED, IRREGULAR, OUT_OF_RANGE, NOT_ENOUGH_DATA }
data class PredictionConfig(
    val manualCycleLength: Int? = null, val manualPeriodLength: Int? = null,
    val initialCycleLength: Int? = null, val initialPeriodLength: Int? = null,
    val lutealPhaseDays: Int = 14, val futureCycles: Int = 6, val showFertility: Boolean = true,
)
data class PredictionResult(
    val cycleLength: Int, val periodLength: Int, val confidence: Confidence, val rangeDays: Int,
    val currentCycleStart: LocalDate?, val cycleDay: Int?, val inPeriod: Boolean,
    val nextPeriodStart: LocalDate?, val daysLate: Int, val predictedPeriods: List<DateRange>,
    val fertileWindows: List<DateRange>, val ovulationDays: List<LocalDate>,
    val fertilityHiddenReason: FertilityHiddenReason?,
)

/** Values stay chronological so the newest six receive the largest weights. */
fun validCycleLengths(periods: List<Period>): List<Int> {
    val lengths = periods.sortedBy { it.start }.zipWithNext { a, b -> (b.start.toEpochDay() - a.start.toEpochDay()).toInt() }
        .takeLast(12).filter { it in 15..90 }
    if (lengths.size < 4) return lengths.takeLast(6)
    val sorted = lengths.sorted()
    val median = if (sorted.size % 2 == 0) (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0 else sorted[sorted.size / 2].toDouble()
    return lengths.filter { abs(it - median) <= 10 }.takeLast(6)
}
fun validPeriodLengths(periods: List<Period>): List<Int> = periods.filter { it.end != null }.takeLast(6)
    .mapNotNull { it.length }.filter { it in 1..14 }
fun populationDeviation(values: List<Int>): Double = if (values.isEmpty()) 0.0 else {
    val mean = values.average(); sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
}

/** Pure local prediction. The original next date never shifts when a period is late. */
fun predict(periods: List<Period>, today: LocalDate, config: PredictionConfig = PredictionConfig()): PredictionResult {
    val history = periods.filter { it.start <= today }.sortedBy { it.start }
    val lengths = validCycleLengths(history)
    val n = lengths.size
    val deviation = populationDeviation(lengths)
    val confidence = when {
        history.isEmpty() -> Confidence.NONE
        n < 3 -> Confidence.LOW
        deviation <= 2 -> Confidence.HIGH
        deviation <= 4 -> Confidence.MEDIUM
        else -> Confidence.LOW
    }
    val cycle = (config.manualCycleLength ?: when {
        n == 0 -> config.initialCycleLength ?: 28
        n < 3 -> ((lengths.sum() + (config.initialCycleLength ?: 28)).toDouble() / (n + 1)).roundToInt()
        else -> (lengths.mapIndexed { index, length -> (index + 1) * length }.sum().toDouble() / (n * (n + 1) / 2)).roundToInt()
    }).coerceIn(15, 90)
    val periodLengths = validPeriodLengths(history)
    val period = (config.manualPeriodLength ?: when (periodLengths.size) {
        0 -> config.initialPeriodLength ?: 5
        1, 2 -> ((periodLengths.sum() + (config.initialPeriodLength ?: 5)).toDouble() / (periodLengths.size + 1)).roundToInt()
        else -> periodLengths.average().roundToInt()
    }).coerceIn(2, 10)
    val range = if (n >= 3) deviation.roundToInt().coerceIn(1, 7) else 3
    val last = history.lastOrNull()
    val start = last?.start
    val cycleDay = start?.let { (today.toEpochDay() - it.toEpochDay() + 1).toInt() }
    val inPeriod = history.any { it.contains(today, today) }
    val next = start?.plusDays(cycle.toLong())
    val late = if (next != null && today > next && !inPeriod) (today.toEpochDay() - next.toEpochDay()).toInt() else 0
    val anchor = next?.let { if (late > 0) today else it }
    val forecasts = if (anchor == null) emptyList() else (0 until config.futureCycles.coerceIn(0, 6)).mapNotNull { k ->
        val from = anchor.plusDays(k.toLong() * cycle)
        val to = from.plusDays(period - 1L)
        if (to < today) null else DateRange(maxOf(from, today), to)
    }
    val reason = when {
        !config.showFertility -> FertilityHiddenReason.DISABLED
        history.isEmpty() -> FertilityHiddenReason.NOT_ENOUGH_DATA
        cycle !in 21..35 -> FertilityHiddenReason.OUT_OF_RANGE
        n >= 3 && lengths.max() - lengths.min() >= 17 -> FertilityHiddenReason.IRREGULAR
        else -> null
    }
    val blocked = history.map { DateRange(it.start, it.effectiveEnd(today)) } + forecasts
    val ovulations = if (reason != null) emptyList() else (listOfNotNull(next) + forecasts.map { it.start }).distinct()
        .map { it.minusDays(config.lutealPhaseDays.toLong().coerceIn(1, 30)) }
    val fertility = ovulations.flatMap { ovulation ->
        val from = maxOf(today, ovulation.minusDays(5))
        val to = ovulation.plusDays(1)
        if (to < from) emptyList() else subtract(DateRange(from, to), blocked)
    }.distinct().sortedBy { it.start }
    return PredictionResult(cycle, period, confidence, range, start, cycleDay, inPeriod, next, late,
        forecasts, fertility, ovulations.filter { it >= today && blocked.none { range -> it in range } }, reason)
}

private fun subtract(range: DateRange, blocked: List<DateRange>): List<DateRange> = blocked.fold(listOf(range)) { parts, cut ->
    parts.flatMap { part ->
        if (cut.end < part.start || cut.start > part.end) listOf(part)
        else buildList {
            if (cut.start > part.start) add(DateRange(part.start, cut.start.minusDays(1)))
            if (cut.end < part.end) add(DateRange(cut.end.plusDays(1), part.end))
        }
    }
}
