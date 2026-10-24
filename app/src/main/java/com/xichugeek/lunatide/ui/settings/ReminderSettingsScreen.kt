package com.xichugeek.lunatide.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.domain.reminder.ReminderKind
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(state: ReminderUiState, granted: Boolean, actions: ReminderActions, onBack: () -> Unit) {
    var showTime by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    val settings = state.settings
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }; Text(stringResource(R.string.reminders), style = MaterialTheme.typography.headlineMedium) }
        if (!state.ready) item { CircularProgressIndicator() }
        if (!granted) item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.notification_permission_off))
                TextButton(onClick = actions.requestPermission) { Text(stringResource(R.string.enable_notifications)) }
                TextButton(onClick = actions.systemSettings) { Text(stringResource(R.string.open_system_settings)) }
            } }
        }
        item { TextButton(onClick = { showTime = true }) { Text(stringResource(R.string.reminder_time, LocalTime.of(settings.reminderTimeMinutes / 60, settings.reminderTimeMinutes % 60).toString())) }; Text(stringResource(R.string.reminder_timing_note), style = MaterialTheme.typography.bodySmall) }
        item { ReminderToggle(R.string.reminder_upcoming, settings.reminderUpcomingEnabled) { actions.enabled(ReminderKind.UPCOMING, it); if (it && !granted) actions.requestPermission() }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1, 2, 3, 5).forEach { days -> FilterChip(settings.reminderDaysBefore == days, { actions.daysBefore(days) }, label = { Text(pluralStringResource(R.plurals.days_before, days, days)) }, modifier = Modifier.heightIn(min = 48.dp)) } }
        }
        item { ReminderToggle(R.string.reminder_start, settings.reminderStartEnabled) { actions.enabled(ReminderKind.START, it); if (it && !granted) actions.requestPermission() } }
        item { ReminderToggle(R.string.reminder_end, settings.reminderEndEnabled) { actions.enabled(ReminderKind.END, it); if (it && !granted) actions.requestPermission() } }
        item { ReminderToggle(R.string.reminder_daily, settings.reminderDailyEnabled) { actions.enabled(ReminderKind.DAILY, it); if (it && !granted) actions.requestPermission() } }
        item { ReminderToggle(R.string.reminder_fertile, settings.reminderFertileEnabled) { actions.enabled(ReminderKind.FERTILE, it); if (it && !granted) actions.requestPermission() }
            if (settings.reminderFertileEnabled && !settings.showFertility) Text(stringResource(R.string.reminder_fertility_off))
        }
        item { ReminderToggle(R.string.notification_private_mode, settings.notificationPrivateMode, actions.privateMode); Text(stringResource(R.string.notification_private_body), style = MaterialTheme.typography.bodySmall) }
        if (state.failed) item { Text(stringResource(R.string.reminder_schedule_error), color = MaterialTheme.colorScheme.error) }
        item { TextButton(onClick = { help = !help }) { Text(stringResource(R.string.reminder_help_title)) }; if (help) { Text(stringResource(R.string.reminder_help)); TextButton(onClick = actions.systemSettings) { Text(stringResource(R.string.open_system_settings)) } } }
    }
    if (showTime) {
        val time = rememberTimePickerState(settings.reminderTimeMinutes / 60, settings.reminderTimeMinutes % 60, is24Hour = true)
        AlertDialog(onDismissRequest = { showTime = false }, title = { Text(stringResource(R.string.reminder_time_title)) },
            text = { TimeInput(time) }, confirmButton = { TextButton(onClick = { actions.time(time.hour * 60 + time.minute); showTime = false }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel)) } })
    }
}
@Composable private fun ReminderToggle(labelId: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val label = stringResource(labelId)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { ReminderSettingsScreen(ReminderUiState(ready = true), true, ReminderActions(), {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { ReminderSettingsScreen(ReminderUiState(ready = true), true, ReminderActions(), {}) } }
