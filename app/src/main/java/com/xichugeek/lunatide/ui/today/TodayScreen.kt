package com.xichugeek.lunatide.ui.today

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.prediction.*
import com.xichugeek.lunatide.ui.components.*
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(state: TodayUiState, busy: Boolean, onRecord: () -> Unit, onSettings: () -> Unit, onDayLog: () -> Unit) {
    val status = state.homeStatus()
    val prediction = state.prediction
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
        HomeStage.WAITING -> prediction?.nextPeriodStart?.let {
            pluralStringResource(R.plurals.predicted_start, prediction.rangeDays, it.format(dateFormat), prediction.rangeDays)
        }.orEmpty()
        HomeStage.DUE -> stringResource(R.string.due_today_body)
        HomeStage.LATE -> stringResource(if (status.days <= 7) R.string.late_common else R.string.late_long)
    }
    val cyclePosition = prediction?.cycleDay?.let { pluralStringResource(R.plurals.cycle_position, it, it) }
    val description = listOfNotNull(cyclePosition, title).joinToString(stringResource(R.string.summary_separator))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.today.format(headerFormat), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onSettings) {
                    Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CycleRing(prediction?.cycleDay, prediction?.cycleLength ?: 28, prediction?.periodLength ?: 5,
                    prediction != null && prediction.fertilityHiddenReason == null, status.stage == HomeStage.LATE, description) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (status.stage) {
                            HomeStage.ONGOING, HomeStage.WAITING, HomeStage.LATE -> {
                                Text(stringResource(when (status.stage) {
                                    HomeStage.ONGOING -> R.string.ring_period
                                    HomeStage.WAITING -> R.string.next_period_prefix
                                    else -> R.string.late_prefix
                                }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                val value = if (status.stage == HomeStage.ONGOING)
                                    pluralStringResource(R.plurals.ring_period_day, status.days, status.days)
                                else pluralStringResource(R.plurals.days_value, status.days, status.days)
                                CycleNumber(value, status.days)
                                if (status.stage == HomeStage.ONGOING) {
                                    if (status.days <= 15) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                } else cyclePosition?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            else -> Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        }
                    }
                }
                Text(if (status.stage == HomeStage.ONGOING && status.days <= 15) stringResource(R.string.period_care_note) else subtitle,
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onRecord() }, enabled = !busy,
                    interactionSource = interaction, shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }) {
                    Text(stringResource(if (status.stage == HomeStage.ONGOING) R.string.period_ended else R.string.period_started), style = MaterialTheme.typography.titleMedium)
                }
                if (state.periods.size == 1 && status.stage != HomeStage.ONGOING)
                    Text(stringResource(R.string.more_records_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { TodayLogSummary(state.logs.firstOrNull { it.day == state.today }, onDayLog) }
        item {
            Text(stringResource(R.string.health_short), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { PredictionDetails(prediction, dateFormat) }
    }
}

@Composable private fun CycleNumber(label: String, value: Int) {
    val number = value.toString()
    val start = label.indexOf(number)
    val large = MaterialTheme.typography.displayMedium
    val text = buildAnnotatedString {
        append(label)
        if (start >= 0) addStyle(SpanStyle(fontSize = large.fontSize, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"), start, start + number.length)
    }
    Text(text, style = MaterialTheme.typography.titleLarge.copy(lineHeight = large.lineHeight), textAlign = TextAlign.Center)
}

@Composable private fun TodayLogSummary(log: DayLog?, onOpen: () -> Unit) {
    val parts = mutableListOf<String>()
    log?.flow?.let { parts += stringResource(R.string.summary_flow, stringResource(flowLabel(it))) }
    log?.pain?.let {
        val severity = stringResource(listOf(R.string.pain_none, R.string.pain_mild, R.string.pain_moderate, R.string.pain_severe)[it])
        parts += if (it == 0) severity else stringResource(R.string.summary_pain, severity)
    }
    log?.mood?.let { parts += stringResource(moodLabel(it)) }
    log?.symptoms?.sortedBy { it.name }?.forEach { parts += stringResource(symptomLabel(it)) }
    val text = when {
        log == null || log.isEmpty -> stringResource(R.string.record_feeling)
        parts.isEmpty() -> stringResource(R.string.daily_record_saved)
        else -> parts.take(3).joinToString(stringResource(R.string.summary_separator))
    }
    Card(onClick = onOpen, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.today_log), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.record_action), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun PredictionDetails(prediction: PredictionResult?, dateFormat: DateTimeFormatter) {
    var expanded by remember { mutableStateOf(false) }
    val reason = when (prediction?.fertilityHiddenReason) {
        FertilityHiddenReason.DISABLED -> R.string.fertility_disabled
        FertilityHiddenReason.IRREGULAR -> R.string.fertility_irregular
        FertilityHiddenReason.OUT_OF_RANGE -> R.string.fertility_out_of_range
        FertilityHiddenReason.NOT_ENOUGH_DATA -> R.string.fertility_no_data
        null -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (reason != null) Text(stringResource(reason), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (prediction?.nextPeriodStart != null) TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (expanded) R.string.hide_predictions else R.string.prediction_details), style = MaterialTheme.typography.bodySmall)
            Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(16.dp).graphicsLayer { rotationZ = if (expanded) 270f else 90f })
        }
        if (expanded) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.coming_up), style = MaterialTheme.typography.titleSmall)
                prediction?.nextPeriodStart?.let { Text(pluralStringResource(R.plurals.predicted_start, prediction.rangeDays, it.format(dateFormat), prediction.rangeDays)) }
                prediction?.fertileWindows?.firstOrNull()?.let {
                    Text(stringResource(R.string.fertile_range, it.start.format(dateFormat), it.end.format(dateFormat)))
                }
            }
        }
    }
}

private fun previewState(): TodayUiState {
    val today = LocalDate.of(2026, 10, 24)
    val periods = listOf(Period(start = today.minusDays(2), end = null))
    return TodayUiState(ready = true, today = today, periods = periods, prediction = predict(periods, today))
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { TodayScreen(previewState(), false, {}, {}, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { TodayScreen(previewState(), false, {}, {}, {}) } }
