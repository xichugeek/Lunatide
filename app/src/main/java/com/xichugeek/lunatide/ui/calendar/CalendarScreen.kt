package com.xichugeek.lunatide.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.prediction.predict
import com.xichugeek.lunatide.ui.components.flowLabel
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import com.xichugeek.lunatide.ui.today.TodayUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun CalendarScreen(state: TodayUiState, calendar: CalendarUiState, onMove: (Int) -> Unit, onToday: () -> Unit, onDay: (LocalDate) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val days = remember(calendar.month, calendar.weekStart) { calendarDays(calendar.month, calendar.weekStart) }
    val logsByDay = remember(state.logs) { state.logs.associateBy { it.day } }
    var drag by remember { mutableFloatStateOf(0f) }
    var legend by remember { mutableStateOf(false) }
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    LazyColumn(Modifier.fillMaxSize().pointerInput(calendar.month) {
        detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = { if (kotlin.math.abs(drag) > 60.dp.toPx()) onMove(if (drag < 0) 1 else -1) }) { _, amount -> drag += amount }
    }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMove(-1) }) { Icon(painterResource(R.drawable.ic_chevron), stringResource(R.string.previous_month), Modifier.graphicsLayer { rotationZ = 180f }) }
                Text(calendar.month.atDay(1).format(DateTimeFormatter.ofPattern(stringResource(R.string.month_date_pattern), locale)), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onMove(1) }) { Icon(painterResource(R.drawable.ic_chevron), stringResource(R.string.next_month)) }
            }
            if (calendar.month != YearMonth.from(state.today)) TextButton(onClick = onToday) { Text(stringResource(R.string.back_to_today)) }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { (0..6).forEach { offset ->
                val weekday = calendar.weekStart.plus(offset.toLong())
                val fullName = weekday.getDisplayName(TextStyle.FULL, locale)
                Text(weekday.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = fullName }, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            days.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) {
                week.forEach { day -> CalendarCell(day, state, day.month == calendar.month.month, logsByDay[day], Modifier.weight(1f)) { onDay(day) } }
            } }
        }
        item {
            TextButton(onClick = { legend = !legend }) { Text(stringResource(R.string.legend)) }
            if (legend) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.recorded_period, R.string.predicted_period, R.string.predicted_fertile, R.string.predicted_ovulation, R.string.flow_spotting, R.string.has_record).forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendMark(index); Text(stringResource(label))
                    }
                }
            }
            Text(stringResource(R.string.health_short), style = MaterialTheme.typography.bodySmall)
        }
        item {
            val first = calendar.month.atDay(1)
            val last = calendar.month.atEndOfMonth()
            val periods = state.periods.filter { it.start <= last && it.effectiveEnd(state.today) >= first }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (periods.isEmpty()) Text(stringResource(R.string.month_no_period))
                    else periods.forEach {
                        val start = maxOf(it.start, first); val end = minOf(it.effectiveEnd(state.today), last)
                        Text(stringResource(R.string.month_period, start.format(dateFormat), end.format(dateFormat)))
                        val count = (end.toEpochDay() - start.toEpochDay() + 1).toInt()
                        Text(pluralStringResource(R.plurals.days_value, count, count))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(day: LocalDate, state: TodayUiState, inMonth: Boolean, log: DayLog?, modifier: Modifier, onClick: () -> Unit) {
    val actual = state.periods.firstOrNull { it.contains(day, state.today) }
    val forecast = day >= state.today && state.prediction?.predictedPeriods?.any { day in it } == true && actual == null
    val fertile = day >= state.today && state.prediction?.fertileWindows?.any { day in it } == true && actual == null && !forecast
    val ovulation = day >= state.today && state.prediction?.ovulationDays?.contains(day) == true
    val spotting = log?.flow == Flow.SPOTTING
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val period = if (dark) Color(0xFFF08C9E) else Color(0xFFB53E57)
    val teal = if (dark) Color(0xFFA5E0E1) else Color(0xFF2E7D80)
    val fill = when {
        actual != null -> period
        forecast -> if (dark) Color(0xFF4A2530) else Color(0xFFFBE3E7)
        fertile -> if (dark) Color(0xFF1F3D3E) else Color(0xFFDDF1F1)
        else -> Color.Transparent
    }
    val foreground = if (actual != null) { if (dark) Color(0xFF35111B) else Color.White } else MaterialTheme.colorScheme.onSurface
    val locale = LocalConfiguration.current.locales[0]
    val labels = mutableListOf(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)))
    if (actual != null) labels += stringResource(R.string.period_day, (day.toEpochDay() - actual.start.toEpochDay() + 1).toInt())
    if (forecast) labels += stringResource(R.string.predicted_period)
    if (fertile) labels += stringResource(R.string.predicted_fertile)
    if (ovulation) labels += stringResource(R.string.predicted_ovulation)
    if (log != null && !log.isEmpty) labels += stringResource(R.string.has_record)
    log?.flow?.let { labels += stringResource(flowLabel(it)) }
    val description = labels.joinToString(" ")
    Box(modifier.heightIn(min = 56.dp).clickable(onClick = onClick).semantics(mergeDescendants = true) { contentDescription = description }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(44.dp).background(fill, CircleShape), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                if (forecast) drawCircle(period, size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
                if (ovulation) {
                    drawCircle(teal, size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    drawCircle(teal, 2.dp.toPx(), Offset(size.width - 3.dp.toPx(), 3.dp.toPx()))
                }
                if (day == state.today) drawLine(foreground, Offset(center.x - 5.dp.toPx(), size.height - 4.dp.toPx()), Offset(center.x + 5.dp.toPx(), size.height - 4.dp.toPx()), 2.dp.toPx())
                if (log != null && !log.isEmpty) drawCircle(foreground, 1.5.dp.toPx(), Offset(center.x, size.height - 1.dp.toPx()))
                if (spotting) {
                    val drop = Path().apply { moveTo(4.dp.toPx(), 4.dp.toPx()); quadraticTo(-2f, 13.dp.toPx(), 4.dp.toPx(), 14.dp.toPx()); quadraticTo(10.dp.toPx(), 13.dp.toPx(), 4.dp.toPx(), 4.dp.toPx()) }
                    drawPath(drop, if (dark) Color(0xFFD9A3AD) else Color(0xFFC98A96))
                }
            }
            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), softWrap = false, color = foreground.copy(alpha = if (inMonth || actual != null) 1f else 0.75f), fontWeight = if (day == state.today) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

@Composable private fun LegendMark(index: Int) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val rose = if (dark) Color(0xFFF08C9E) else Color(0xFFB53E57)
    val teal = if (dark) Color(0xFFA5E0E1) else Color(0xFF2E7D80)
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        when (index) {
            0 -> drawCircle(rose)
            1 -> drawCircle(rose, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
            2 -> drawCircle(if (dark) Color(0xFF1F3D3E) else Color(0xFFDDF1F1))
            3 -> { drawCircle(teal, style = Stroke(1.5.dp.toPx())); drawCircle(teal, 2.dp.toPx(), Offset(size.width - 2.dp.toPx(), 2.dp.toPx())) }
            4 -> drawPath(Path().apply { moveTo(center.x, 2.dp.toPx()); quadraticTo(0f, size.height, center.x, size.height); quadraticTo(size.width, size.height, center.x, 2.dp.toPx()) }, rose)
            5 -> drawCircle(foreground, 2.dp.toPx())
        }
    }
}

private fun previewCalendarState(): TodayUiState {
    val today = LocalDate.of(2026, 10, 24)
    val periods = listOf(Period(start = LocalDate.of(2026, 10, 3), end = LocalDate.of(2026, 10, 8)))
    return TodayUiState(ready = true, periods = periods, today = today, settings = AppSettings(onboardingCompleted = true), prediction = predict(periods, today))
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { CalendarScreen(previewCalendarState(), CalendarUiState(YearMonth.of(2026, 10)), {}, {}, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { CalendarScreen(previewCalendarState(), CalendarUiState(YearMonth.of(2026, 10)), {}, {}, {}) } }
