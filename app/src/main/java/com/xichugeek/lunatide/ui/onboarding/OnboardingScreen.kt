package com.xichugeek.lunatide.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.ui.components.PastDatePicker
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import java.time.LocalDate

@Composable
fun OnboardingScreen(today: LocalDate, state: OnboardingState, onFinish: (LocalDate?, Int?, Int?) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var lastDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var period by rememberSaveable { mutableIntStateOf(5) }
    var cycle by rememberSaveable { mutableIntStateOf(28) }
    var periodKnown by rememberSaveable { mutableStateOf(false) }
    var cycleKnown by rememberSaveable { mutableStateOf(false) }
    var chooseDate by remember { mutableStateOf(false) }
    fun finish() = onFinish(lastDay?.let(LocalDate::ofEpochDay), cycle.takeIf { cycleKnown }, period.takeIf { periodKnown })
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.onboarding_step, page + 1), style = MaterialTheme.typography.labelLarge)
                    TextButton(enabled = !state.busy, onClick = { onFinish(null, null, null) }) { Text(stringResource(R.string.skip)) }
                }
                Spacer(Modifier.height(16.dp))
                when (page) {
                    0 -> {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.headlineSmall)
                        listOf(R.string.promise_account, R.string.promise_local, R.string.promise_quiet).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge) }
                    }
                    1 -> {
                        Text(stringResource(R.string.last_start_question), style = MaterialTheme.typography.headlineMedium)
                        OutlinedButton(onClick = { chooseDate = true }) { Text(lastDay?.let { LocalDate.ofEpochDay(it).toString() } ?: stringResource(R.string.choose_date)) }
                        TextButton(onClick = { lastDay = null; page++ }) { Text(stringResource(R.string.unknown_date)) }
                    }
                    2 -> {
                        Text(stringResource(R.string.cycle_question), style = MaterialTheme.typography.headlineMedium)
                        LengthSelector(R.string.period_length_question, period, 2..10, periodKnown, { period = it; periodKnown = true }, { periodKnown = false })
                        LengthSelector(R.string.cycle_length_question, cycle, 20..45, cycleKnown, { cycle = it; cycleKnown = true }, { cycleKnown = false })
                    }
                    3 -> {
                        Text(stringResource(R.string.security_optional), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.security_optional_body), style = MaterialTheme.typography.bodyLarge)
                    }
                    4 -> {
                        Text(stringResource(R.string.data_notice), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.data_notice_body), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (state.failed) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
                if (state.busy) CircularProgressIndicator()
                Button(onClick = { if (page < 4) page++ else finish() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(if (page == 0) R.string.start else if (page == 4) R.string.done else R.string.next))
                }
                if (page > 0) TextButton(onClick = { page-- }, enabled = !state.busy) { Text(stringResource(R.string.back)) }
            }
        }
    }
    if (chooseDate) PastDatePicker(today, lastDay?.let(LocalDate::ofEpochDay) ?: today.minusDays(28), onDismiss = { chooseDate = false }) { lastDay = it.toEpochDay(); chooseDate = false }
}

@Composable
private fun LengthSelector(title: Int, value: Int, range: IntRange, known: Boolean, onChange: (Int) -> Unit, onUnknown: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
