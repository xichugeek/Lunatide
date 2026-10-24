package com.xichugeek.lunatide.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.ui.components.PastDatePicker
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import java.time.LocalDate

@Composable
fun OnboardingScreen(today: LocalDate, state: OnboardingState, onDraft: (OnboardingDraft) -> Unit = {},
    pinSet: Boolean = false, onPin: () -> Unit = {},
    remindersEnabled: Boolean = false, onReminderToggle: (Boolean) -> Unit = {}, onFinish: (LocalDate?, Int?, Int?) -> Unit) {
    val draft = state.draft
    val page = draft.page
    val lastDay = draft.start
    var chooseDate by remember { mutableStateOf(false) }
    fun finish() = onFinish(lastDay, draft.cycle.takeIf { draft.cycleKnown }, draft.period.takeIf { draft.periodKnown })
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.onboarding_step, page + 1), style = MaterialTheme.typography.labelLarge)
                    TextButton(enabled = !state.busy, onClick = { onFinish(null, null, null) }) { Text(stringResource(R.string.skip)) }
                }
                LinearProgressIndicator(progress = { (page + 1) / 5f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                when (page) {
                    0 -> {
                        Icon(painterResource(R.drawable.ic_moon_wave), null, Modifier.size(104.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.headlineSmall)
                        listOf(R.string.promise_account, R.string.promise_local, R.string.promise_quiet).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge) }
                    }
                    1 -> {
                        Text(stringResource(R.string.last_start_question), style = MaterialTheme.typography.headlineMedium)
                        OutlinedButton(onClick = { chooseDate = true }) { Text(lastDay?.toString() ?: stringResource(R.string.choose_date)) }
                        TextButton(onClick = { onDraft(draft.copy(start = null, page = page + 1)) }) { Text(stringResource(R.string.unknown_date)) }
                    }
                    2 -> {
                        Text(stringResource(R.string.cycle_question), style = MaterialTheme.typography.headlineMedium)
                        LengthSelector(R.string.period_length_question, draft.period, 2..10, draft.periodKnown, { onDraft(draft.copy(period = it, periodKnown = true)) }, { onDraft(draft.copy(periodKnown = false)) })
                        LengthSelector(R.string.cycle_length_question, draft.cycle, 20..45, draft.cycleKnown, { onDraft(draft.copy(cycle = it, cycleKnown = true)) }, { onDraft(draft.copy(cycleKnown = false)) })
                    }
                    3 -> {
                        Text(stringResource(R.string.security_optional), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.security_optional_body), style = MaterialTheme.typography.bodyLarge)
                        if (pinSet) Text(stringResource(R.string.pin_enabled)) else OutlinedButton(onClick = onPin) { Text(stringResource(R.string.pin_enable)) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.reminder_upcoming), Modifier.weight(1f))
                            val label = stringResource(R.string.reminder_upcoming)
                            Switch(remindersEnabled, onReminderToggle, modifier = Modifier.semantics { contentDescription = label })
                        }
                    }
                    4 -> {
                        Text(stringResource(R.string.data_notice), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.data_notice_body), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (state.failed) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
                if (state.busy) CircularProgressIndicator()
                Button(onClick = { if (page < 4) onDraft(draft.copy(page = page + 1)) else finish() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(if (page == 0) R.string.start else if (page == 4) R.string.done else R.string.next))
                }
                if (page > 0) TextButton(onClick = { onDraft(draft.copy(page = page - 1)) }, enabled = !state.busy) { Text(stringResource(R.string.back)) }
            }
        }
    }
    if (chooseDate) PastDatePicker(today, lastDay ?: today.minusDays(28), onDismiss = { chooseDate = false }) { onDraft(draft.copy(start = it)); chooseDate = false }
}

@Composable
private fun LengthSelector(title: Int, value: Int, range: IntRange, known: Boolean, onChange: (Int) -> Unit, onUnknown: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onChange(value - 1) }, enabled = value > range.first) { Text(stringResource(R.string.decrease)) }
            Text(pluralStringResource(R.plurals.days_value, value, value))
            OutlinedButton(onClick = { onChange(value + 1) }, enabled = value < range.last) { Text(stringResource(R.string.increase)) }
        }
        TextButton(onClick = onUnknown) { Text(stringResource(R.string.unknown)) }
        if (!known) Text(stringResource(R.string.default_unknown), style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { OnboardingScreen(LocalDate.of(2026, 10, 24), OnboardingState()) { _, _, _ -> } } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { OnboardingScreen(LocalDate.of(2026, 10, 24), OnboardingState()) { _, _, _ -> } } }
