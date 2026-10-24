package com.xichugeek.lunatide.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.stats.*
import com.xichugeek.lunatide.ui.components.symptomLabel
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun StatsScreen(state: StatsUiState, onHistory: (LocalDate) -> Unit) {
    val stats = state.stats
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(stringResource(R.string.statistics), style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(6.dp)); Text(stringResource(R.string.statistics_intro), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        if (state.failed) item { Text(stringResource(R.string.storage_error)) }
        else if (!state.ready) item { CircularProgressIndicator() }
        else if (stats == null || stats.cycles.size < 2) item {
            StatsCard(R.string.empty_statistics) { Text(stringResource(R.string.empty_statistics_body)) }
        } else {
            item { StatsCard(R.string.overview) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatNumber(R.string.average_cycle, stats.avgCycleLength)
                    StatNumber(R.string.average_period, stats.avgPeriodLength)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.regularity), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(when (stats.regularity) { Regularity.REGULAR -> R.string.regular; Regularity.SOMEWHAT_REGULAR -> R.string.somewhat_regular; Regularity.IRREGULAR -> R.string.irregular; null -> R.string.not_enough_data }), style = MaterialTheme.typography.titleLarge)
                    }
                }
            } }
            item { StatsCard(R.string.cycle_chart) { HistoryChart(stats.cycles.mapNotNull { cycle -> cycle.cycleLength?.let { ChartValue(cycle.start, it) } }.takeLast(12), stats.avgCycleLength, true) } }
            item { StatsCard(R.string.period_chart) { HistoryChart(stats.cycles.mapNotNull { cycle -> cycle.periodLength?.let { ChartValue(cycle.start, it) } }.takeLast(12), stats.avgPeriodLength, false) } }
            item { StatsCard(R.string.common_symptoms) {
                if (stats.topSymptoms.isEmpty()) Text(stringResource(R.string.no_symptoms))
                stats.topSymptoms.forEach { (symptom, count) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(symptomLabel(symptom)), modifier = Modifier.weight(1f))
                        Text(pluralStringResource(R.plurals.occurrences, count, count))
                    }
                }
            } }
            item { StatsCard(R.string.pain_distribution) {
                listOf(R.string.pain_none, R.string.pain_mild, R.string.pain_moderate, R.string.pain_severe).forEachIndexed { index, label ->
                    val count = stats.painDistribution[index] ?: 0
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(label), modifier = Modifier.weight(1f)); Text(pluralStringResource(R.plurals.occurrences, count, count))
                    }
                }
            } }
            item { Text(stringResource(R.string.cycle_history), style = MaterialTheme.typography.titleLarge) }
            items(stats.cycles.asReversed(), key = { it.start.toEpochDay() }) { cycle ->
                Card(onClick = { onHistory(cycle.start) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.started_on, cycle.start.format(dateFormat)), style = MaterialTheme.typography.titleMedium)
                        if (cycle.cycleLength == null) Text(stringResource(R.string.cycle_in_progress, (state.today.toEpochDay() - cycle.start.toEpochDay() + 1).toInt()))
                        else Text(stringResource(R.string.history_cycle, pluralStringResource(R.plurals.days_value, cycle.cycleLength, cycle.cycleLength)))
                        if (cycle.periodLength != null) Text(stringResource(R.string.history_period, pluralStringResource(R.plurals.days_value, cycle.periodLength, cycle.periodLength)))
                        else Text(stringResource(R.string.period_in_progress))
                    }
                }
            }
            stats.gentleTips.forEach { tip -> item { Text(stringResource(tipLabel(tip)), style = MaterialTheme.typography.bodyMedium) } }
        }
        item { Text(stringResource(R.string.health_full), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun StatNumber(label: Int, value: Double?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Text(value?.let { pluralStringResource(R.plurals.days_value, it.roundToInt(), it.roundToInt()) } ?: stringResource(R.string.not_enough_data), style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.primary)
    }
}
@Composable private fun StatsCard(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium); content()
        }
    }
}
private fun tipLabel(tip: GentleTip): Int = when (tip) {
    GentleTip.CYCLE_OUT_OF_COMMON_RANGE -> R.string.tip_cycle_range
    GentleTip.LONG_PERIODS -> R.string.tip_long_periods
    GentleTip.IRREGULAR_CYCLES -> R.string.tip_irregular
    GentleTip.SEVERE_PAIN -> R.string.tip_pain
}
private fun sampleState(): StatsUiState {
    val start = LocalDate.of(2026, 2, 1)
    val periods = (0 until 8).map { index -> val day = start.plusDays(28L * index); Period(start = day, end = day.plusDays(4 + index % 2L)) }
    val today = LocalDate.of(2026, 10, 24)
    return StatsUiState(true, today = today, stats = computeStats(periods, periods.takeLast(3).map { DayLog(it.start, pain = 1, symptoms = setOf(Symptom.FATIGUE)) }, today))
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { StatsScreen(sampleState(), {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { StatsScreen(sampleState(), {}) } }
