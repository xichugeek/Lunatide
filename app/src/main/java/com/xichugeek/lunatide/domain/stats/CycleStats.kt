package com.xichugeek.lunatide.domain.stats

import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.prediction.*
import java.time.LocalDate

enum class Regularity { REGULAR, SOMEWHAT_REGULAR, IRREGULAR }
enum class GentleTip { CYCLE_OUT_OF_COMMON_RANGE, LONG_PERIODS, IRREGULAR_CYCLES, SEVERE_PAIN }
data class CycleSummary(val start: LocalDate, val cycleLength: Int?, val periodLength: Int?)
data class CycleStats(
    val cycles: List<CycleSummary>, val avgCycleLength: Double?, val avgPeriodLength: Double?,
    val regularity: Regularity?, val topSymptoms: List<Pair<Symptom, Int>>,
    val painDistribution: Map<Int, Int>, val gentleTips: List<GentleTip>,
)

fun computeStats(periods: List<Period>, dayLogs: List<DayLog>, today: LocalDate): CycleStats {
    val history = periods.filter { it.start <= today }.sortedBy { it.start }
    val lengths = validCycleLengths(history)
    val periodLengths = validPeriodLengths(history)
    val averageCycle = lengths.takeIf { it.isNotEmpty() }?.average()
    val averagePeriod = periodLengths.takeIf { it.isNotEmpty() }?.average()
    val regularity = if (lengths.size < 3) null else when (lengths.max() - lengths.min()) {
        in 0..9 -> Regularity.REGULAR
        in 10..16 -> Regularity.SOMEWHAT_REGULAR
        else -> Regularity.IRREGULAR
    }
    val from = history.takeLast(6).firstOrNull()?.start
    val recentLogs = if (from == null) emptyList() else dayLogs.filter { it.day in from..today }
    val symptoms = recentLogs.flatMap { it.symptoms }.groupingBy { it }.eachCount().toList()
        .sortedWith(compareByDescending<Pair<Symptom, Int>> { it.second }.thenBy { it.first.name }).take(5)
    val pain = (0..3).associateWith { score -> recentLogs.count { it.pain == score } }
    val severePeriods = history.takeLast(3).count { period ->
        recentLogs.any { it.pain == 3 && period.contains(it.day, today) }
    }
    val tips = if (lengths.size < 3) emptyList() else buildList {
        if (averageCycle != null && (averageCycle < 21 || averageCycle > 35)) add(GentleTip.CYCLE_OUT_OF_COMMON_RANGE)
        if (averagePeriod != null && averagePeriod > 8) add(GentleTip.LONG_PERIODS)
        if (regularity == Regularity.IRREGULAR) add(GentleTip.IRREGULAR_CYCLES)
        if (severePeriods >= 2) add(GentleTip.SEVERE_PAIN)
    }
    return CycleStats(history.mapIndexed { index, period ->
        CycleSummary(period.start, history.getOrNull(index + 1)?.let { (it.start.toEpochDay() - period.start.toEpochDay()).toInt() }, period.length)
    }, averageCycle, averagePeriod, regularity, symptoms, pain, tips)
}
