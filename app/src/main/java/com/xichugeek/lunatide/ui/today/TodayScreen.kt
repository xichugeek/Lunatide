package com.xichugeek.lunatide.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.domain.model.Period
import com.xichugeek.lunatide.domain.prediction.*
import com.xichugeek.lunatide.ui.components.CycleRing
import com.xichugeek.lunatide.ui.components.flowLabel
import com.xichugeek.lunatide.ui.components.moodLabel
import com.xichugeek.lunatide.ui.components.symptomLabel
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TodayScreen(state: TodayUiState, busy: Boolean, onRecord: () -> Unit, onSettings: () -> Unit, onDayLog: () -> Unit) {
    val status = state.homeStatus()
    val p = state.prediction
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(180), label = "recordButton")
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = DateTimeFormatter.ofPattern(stringResource(R.string.short_date_pattern), locale)
    val headerFormat = DateTimeFormatter.ofPattern(stringResource(R.string.header_date_pattern), locale)
    val title = when (status.stage) {
        HomeStage.WELCOME -> stringResource(R.string.welcome)
        HomeStage.ONGOING -> stringResource(R.string.period_day, status.days)
        HomeStage.WAITING -> pluralStringResource(R.plurals.days_until, status.days, status.days)
        HomeStage.DUE -> stringResource(R.string.due_today)
        HomeStage.LATE -> pluralStringResource(R.plurals.days_late, status.days, status.days)
    }
    val subtitle = when (status.stage) {
        HomeStage.WELCOME -> stringResource(R.string.welcome_body)
        HomeStage.ONGOING -> when {
            status.days > 15 -> stringResource(R.string.confirm_long_period)
            status.remaining > 0 -> pluralStringResource(R.plurals.period_remaining, status.remaining, status.remaining)
            else -> stringResource(R.string.period_maybe_ended)
        }
        HomeStage.WAITING -> p?.nextPeriodStart?.let { pluralStringResource(R.plurals.predicted_start, p.rangeDays, it.format(dateFormat), p.rangeDays) }.orEmpty()
        HomeStage.DUE -> stringResource(R.string.due_today_body)
        HomeStage.LATE -> stringResource(if (status.days <= 7) R.string.late_common else R.string.late_long)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_today), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text(state.today.format(headerFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onSettings) { Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings), tint = MaterialTheme.colorScheme.primary) }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF292842), contentColor = Color(0xFFF5F2FF), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.your_rhythm), style = MaterialTheme.typography.labelLarge, color = Color(0xFFC6C3DE))
                        Text(stringResource(R.string.local_only_badge), style = MaterialTheme.typography.labelSmall, color = Color(0xFFC6C3DE))
                    }
                    CycleRing(p?.cycleDay, p?.cycleLength ?: 28, p?.periodLength ?: 5, p?.fertilityHiddenReason == null && p != null, status.stage == HomeStage.LATE, title, night = true) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val numeric = status.stage in listOf(HomeStage.ONGOING, HomeStage.WAITING, HomeStage.LATE)
                            if (numeric) {
                                Text(stringResource(when (status.stage) { HomeStage.ONGOING -> R.string.period_day_prefix; HomeStage.WAITING -> R.string.next_period_prefix; else -> R.string.late_prefix }), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD4D0E7), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text(status.days.toString(), style = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp, fontFeatureSettings = "tnum"), fontWeight = FontWeight.Light)
                                Text(pluralStringResource(R.plurals.day_unit, status.days), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD4D0E7))
                            } else {
                                Icon(painterResource(R.drawable.ic_today), null, Modifier.size(32.dp), tint = Color(0xFFF4D6DB))
                                Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    Text(subtitle, textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0DCEC))
                    Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onRecord() }, enabled = !busy,
                        interactionSource = interaction, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E5FC), contentColor = Color(0xFF302D55)),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }) {
                        Text(stringResource(if (status.stage == HomeStage.ONGOING) R.string.period_ended else R.string.period_started), style = MaterialTheme.typography.titleMedium)
                    }
                    if (state.periods.size == 1 && status.stage != HomeStage.ONGOING) Text(stringResource(R.string.more_records_help), style = MaterialTheme.typography.bodySmall, color = Color(0xFFC6C3DE))
                }
            }
        }
        item {
            Card(onClick = onDayLog, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.today_log), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.record_action), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    val log = state.logs.firstOrNull { it.day == state.today }
                    if (log == null || log.isEmpty) Text(stringResource(R.string.record_feeling), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    else {
                        val count = log.symptoms.size + listOf(log.flow, log.pain, log.mood, log.bbtCentiCelsius, log.intercourse, log.note).count { it != null }
                        Text(pluralStringResource(R.plurals.record_summary, count, count))
                        log.flow?.let { Text(stringResource(flowLabel(it))) }
                        log.mood?.let { Text(stringResource(moodLabel(it))) }
                        log.symptoms.sortedBy { it.name }.take(3).forEach { Text(stringResource(symptomLabel(it))) }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.coming_up), style = MaterialTheme.typography.titleMedium)
                    p?.nextPeriodStart?.let { Text(pluralStringResource(R.plurals.predicted_start, p.rangeDays, it.format(dateFormat), p.rangeDays)) }
                    if (p != null) {
                        val reason = when (p.fertilityHiddenReason) {
                            FertilityHiddenReason.DISABLED -> R.string.fertility_disabled
                            FertilityHiddenReason.IRREGULAR -> R.string.fertility_irregular
                            FertilityHiddenReason.OUT_OF_RANGE -> R.string.fertility_out_of_range
                            FertilityHiddenReason.NOT_ENOUGH_DATA -> R.string.fertility_no_data
                            null -> null
                        }
                        if (reason != null) Text(stringResource(reason))
                        else p.fertileWindows.firstOrNull()?.let { Text(stringResource(R.string.fertile_range, it.start.format(dateFormat), it.end.format(dateFormat))) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.health_short), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun previewState(): TodayUiState {
    val today = LocalDate.of(2026, 10, 24)
    val periods = listOf(Period(start = today.minusDays(2), end = null))
    return TodayUiState(ready = true, today = today, periods = periods, prediction = predict(periods, today))
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { TodayScreen(previewState(), false, {}, {}, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { TodayScreen(previewState(), false, {}, {}, {}) } }
