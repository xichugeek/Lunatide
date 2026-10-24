package com.xichugeek.lunatide.ui.daylog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.prediction.PredictionResult
import com.xichugeek.lunatide.ui.components.*
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayLogSheet(state: DayEditorState, settings: AppSettings, prediction: PredictionResult?, actions: DayEditorActions) {
    val windowSize = LocalWindowInfo.current.containerSize
    val maximumHeight = with(LocalDensity.current) { windowSize.height.toDp() * 0.9f }
    ModalBottomSheet(onDismissRequest = actions.close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = maximumHeight)
            .verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.day.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.close) { Text(stringResource(R.string.close)) }
            }
            if (state.loading) CircularProgressIndicator()
            else if (state.future) {
                Text(stringResource(R.string.future_day))
                if (prediction?.predictedPeriods?.any { state.day in it } == true) Text(stringResource(R.string.predicted_period))
                if (prediction?.fertileWindows?.any { state.day in it } == true) Text(stringResource(R.string.predicted_fertile))
                if (prediction?.ovulationDays?.contains(state.day) == true) Text(stringResource(R.string.predicted_ovulation))
                Text(stringResource(R.string.health_short))
            } else {
                val enabled = !state.busy
                if (state.log.isEmpty) Text(stringResource(R.string.no_record), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    val periodLabel = stringResource(R.string.this_day_period)
                    Text(periodLabel, modifier = Modifier.weight(1f))
                    Switch(state.inPeriod, actions.period, enabled = enabled, modifier = Modifier.semantics { contentDescription = periodLabel })
                }
                FieldLabel(R.string.flow)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Flow.entries.forEach { value -> FilterChip(state.log.flow == value, { actions.flow(value.takeUnless { it == state.log.flow }) }, label = { Text(stringResource(flowLabel(value))) }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) }
                }
                FieldLabel(R.string.pain)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.pain_none, R.string.pain_mild, R.string.pain_moderate, R.string.pain_severe).forEachIndexed { score, label ->
                        FilterChip(state.log.pain == score, { actions.pain(score.takeUnless { it == state.log.pain }) }, label = { Text(stringResource(label)) }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                FieldLabel(R.string.symptoms)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Symptom.entries.forEach { value -> FilterChip(value in state.log.symptoms, { actions.symptom(value) }, label = { Text(stringResource(symptomLabel(value))) }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) }
                }
                FieldLabel(R.string.mood)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mood.entries.forEach { value -> FilterChip(state.log.mood == value, { actions.mood(value.takeUnless { it == state.log.mood }) }, label = { Text(stringResource(moodLabel(value))) }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) }
                }
                if (settings.showBbt) {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.temperature)) }
                    if (expanded || state.temperatureInvalid) {
                        PrivateTextInput(state.temperatureText, stringResource(R.string.temperature_hint), decimal = true, enabled = enabled, onChange = actions.temperature)
                        if (state.temperatureInvalid) Text(stringResource(R.string.temperature_invalid), color = MaterialTheme.colorScheme.error)
                    }
                }
                if (settings.showIntercourse) {
                    FieldLabel(R.string.intercourse)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Intercourse.entries.forEach { value -> FilterChip(state.log.intercourse == value,
                            { actions.intercourse(value.takeUnless { it == state.log.intercourse }) },
                            label = { Text(stringResource(if (value == Intercourse.PROTECTED) R.string.protected_intercourse else R.string.unprotected_intercourse)) }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) }
                    }
                }
                FieldLabel(R.string.note)
                PrivateTextInput(state.log.note.orEmpty(), stringResource(R.string.note_hint), enabled = enabled, onChange = actions.note)
                Text(stringResource(R.string.note_count, state.log.note?.length ?: 0), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = actions.clear, enabled = enabled) { Text(stringResource(R.string.clear_day), color = MaterialTheme.colorScheme.error) }
            }
            if (state.saving || state.busy) Text(stringResource(R.string.saving))
            if (state.failed) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(24.dp))
        }
    }
    state.prompt?.let { prompt ->
        val title = when (prompt) { EditorPrompt.INCLUDE_FOR_FLOW -> R.string.include_for_flow; EditorPrompt.RECENT -> R.string.recent_period_title; EditorPrompt.SHORTEN -> R.string.shorten_period; EditorPrompt.CLEAR -> R.string.clear_day }
        val body = when (prompt) { EditorPrompt.INCLUDE_FOR_FLOW -> R.string.include_for_flow_body; EditorPrompt.RECENT -> R.string.include_for_flow_body; EditorPrompt.SHORTEN -> R.string.shorten_period_body; EditorPrompt.CLEAR -> R.string.clear_day_confirm }
        AlertDialog(onDismissRequest = actions.cancel, title = { Text(stringResource(title)) }, text = {
            Column {
                Text(if (prompt == EditorPrompt.RECENT) pluralStringResource(R.plurals.recent_period_body, state.recentGap, state.recentGap) else stringResource(body))
                if (prompt == EditorPrompt.RECENT) TextButton(onClick = actions.spotting) { Text(stringResource(R.string.record_spotting)) }
            }
        }, confirmButton = { TextButton(onClick = actions.confirm) { Text(stringResource(R.string.confirm)) } }, dismissButton = { TextButton(onClick = actions.cancel) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable private fun FieldLabel(label: Int) { Text(stringResource(label), style = MaterialTheme.typography.titleSmall) }
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { DayLogSheet(DayEditorState(LocalDate.of(2026, 10, 24), loading = false), AppSettings(), null, DayEditorActions()) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { DayLogSheet(DayEditorState(LocalDate.of(2026, 10, 24), loading = false), AppSettings(), null, DayEditorActions()) } }
