package com.xichugeek.lunatide.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

data class ChartValue(val start: LocalDate, val days: Int)

@Composable
fun HistoryChart(values: List<ChartValue>, average: Double?, commonBand: Boolean) {
    if (values.isEmpty()) { Text(stringResource(R.string.no_complete_records)); return }
    val primary = MaterialTheme.colorScheme.primary
    val band = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = max(values.maxOf { it.days }, if (commonBand) 40 else 10)
    val locale = LocalConfiguration.current.locales[0]
    val labelFormat = DateTimeFormatter.ofPattern("M/d", locale)
    val fullDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale)
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Box(Modifier.width((values.size * 56).dp).height(180.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val baseline = size.height
                val scale = 140.dp.toPx() / maxValue
                if (commonBand) drawRect(band, Offset(0f, baseline - 35 * scale), Size(size.width, 14 * scale))
                average?.let { drawLine(line, Offset(0f, baseline - it.toFloat() * scale), Offset(size.width, baseline - it.toFloat() * scale), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))) }
            }
            Row(Modifier.matchParentSize()) {
                values.forEach { value ->
                    val description = stringResource(R.string.chart_bar_description, value.start.format(fullDateFormat), pluralStringResource(R.plurals.days_value, value.days, value.days))
                    Column(Modifier.width(56.dp).fillMaxHeight().semantics { contentDescription = description }, verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value.days.toString(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.clearAndSetSemantics { })
                        Canvas(Modifier.width(26.dp).height((140f * value.days / maxValue).coerceAtLeast(2f).dp).clearAndSetSemantics { }) {
                            drawRoundRect(primary, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                        }
                    }
                }
            }
        }
        Row(Modifier.width((values.size * 56).dp)) {
            values.forEach { Text(it.start.format(labelFormat), modifier = Modifier.width(56.dp).clearAndSetSemantics { }, textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall) }
        }
    }
    Text(stringResource(if (commonBand) R.string.cycle_chart_note else R.string.period_chart_note), style = MaterialTheme.typography.bodySmall)
}
