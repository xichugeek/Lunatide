package com.xichugeek.lunatide.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.prediction.predict
import com.xichugeek.lunatide.domain.usecase.*
import com.xichugeek.lunatide.ui.components.PastDatePicker
import com.xichugeek.lunatide.ui.onboarding.*
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import com.xichugeek.lunatide.ui.today.*
import com.xichugeek.lunatide.ui.calendar.CalendarScreen
import com.xichugeek.lunatide.ui.calendar.CalendarUiState
import com.xichugeek.lunatide.ui.daylog.*
import com.xichugeek.lunatide.ui.stats.*
import com.xichugeek.lunatide.ui.settings.*
import com.xichugeek.lunatide.domain.reminder.ReminderKind
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable data object Today
@Serializable data object Calendar
@Serializable data object Statistics
@Serializable data object Settings
@Serializable data object Reminders

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunatideRoot(
    state: TodayUiState, actions: ActionState, onboarding: OnboardingState,
    onFinish: (LocalDate?, Int?, Int?) -> Unit, onRecord: (LocalDate, Boolean, Boolean) -> Unit,
    onUndo: () -> Unit, onSpotting: () -> Unit, onClear: () -> Unit, onRetry: () -> Unit,
    calendar: CalendarUiState = CalendarUiState(java.time.YearMonth.from(state.today)),
    onMoveMonth: (Int) -> Unit = {}, onCalendarToday: () -> Unit = {},
    editor: DayEditorState? = null, editorActions: DayEditorActions = DayEditorActions(), onOpenDay: (LocalDate) -> Unit = {},
    stats: StatsUiState = StatsUiState(), onShowMonth: (LocalDate) -> Unit = {},
    reminder: ReminderUiState = ReminderUiState(), reminderActions: ReminderActions = ReminderActions(),
    notificationsGranted: Boolean = false, onOnboardingDraft: (OnboardingDraft) -> Unit = {},
) {
    if (state.failed) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.storage_error)); Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }; return
    }
    if (!state.ready) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    if (!state.settings.onboardingCompleted) {
        OnboardingScreen(state.today, onboarding, onDraft = onOnboardingDraft,
            remindersEnabled = state.settings.reminderUpcomingEnabled && notificationsGranted,
            onReminderToggle = { enabled -> reminderActions.enabled(ReminderKind.UPCOMING, enabled); if (enabled && !notificationsGranted) reminderActions.requestPermission() }, onFinish = onFinish)
        return
    }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var changeDate by remember { mutableStateOf(false) }
    Scaffold(bottomBar = {
        if (route !in listOf(Settings::class.qualifiedName, Reminders::class.qualifiedName)) NavigationBar {
            val destinations = listOf(Triple(Today, R.string.today, R.drawable.ic_today), Triple(Calendar, R.string.calendar, R.drawable.ic_calendar), Triple(Statistics, R.string.statistics, R.drawable.ic_stats))
            destinations.forEach { (destination, label, icon) ->
                NavigationBarItem(selected = route == destination::class.qualifiedName,
                    onClick = { nav.navigate(destination) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(painterResource(icon), null) }, label = { Text(stringResource(label)) })
            }
        }
    }, snackbarHost = {
        actions.notice?.let { notice ->
            Snackbar(Modifier.padding(12.dp)) {
                Column {
                    val day = if (notice.day == state.today) stringResource(R.string.today) else notice.day.toString()
                    Text(stringResource(if (notice.action == PeriodAction.START) R.string.recorded_start else R.string.recorded_end, day))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
                        TextButton(onClick = onUndo, colors = colors) { Text(stringResource(R.string.undo)) }
                        TextButton(onClick = { changeDate = true }, colors = colors) { Text(stringResource(R.string.change_date)) }
                        TextButton(onClick = onClear, colors = colors) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            NavHost(nav, startDestination = Today, modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                composable<Today> { TodayScreen(state, actions.busy, { onRecord(state.today, false, false) }, { nav.navigate(Settings) }, { onClear(); onOpenDay(state.today) }) }
                composable<Calendar> { CalendarScreen(state, calendar, onMoveMonth, onCalendarToday) { onClear(); onOpenDay(it) } }
                composable<Statistics> { StatsScreen(stats) { onShowMonth(it); nav.navigate(Calendar) } }
                composable<Settings> {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.back)) }
                        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
                        TextButton(onClick = { nav.navigate(Reminders) }) { Text(stringResource(R.string.reminders)) }
                        Text(stringResource(R.string.promise_local)); Text(stringResource(R.string.data_notice_body)); Text(stringResource(R.string.health_full))
                    }
                }
                composable<Reminders> { ReminderSettingsScreen(reminder, notificationsGranted, reminderActions) { nav.popBackStack() } }
            }
        }
    }
    if (changeDate) actions.notice?.let { notice ->
        val minimum = if (notice.action == PeriodAction.END) notice.before.lastOrNull { it.end == null }?.start
            else notice.before.mapNotNull { it.end }.maxOrNull()?.plusDays(1)
        PastDatePicker(state.today, notice.day, minimum, { changeDate = false }) { onRecord(it, false, true); changeDate = false }
    }
    if (actions.issue == EditIssue.CONFIRM_RECENT) {
        val end = state.periods.mapNotNull { it.end }.maxOrNull()
        AlertDialog(onDismissRequest = onClear, title = { Text(stringResource(R.string.recent_period_title)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val gap = end?.let { (state.today.toEpochDay() - it.toEpochDay()).toInt() } ?: 0
                Text(pluralStringResource(R.plurals.recent_period_body, gap, gap))
                TextButton(onClick = onSpotting) { Text(stringResource(R.string.record_spotting)) }
            } }, confirmButton = { TextButton(onClick = { onRecord(state.today, true, false) }) { Text(stringResource(R.string.new_period_confirm)) } },
            dismissButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.cancel)) } })
    } else if (actions.issue != null || actions.storageError) {
        val message = when (actions.issue) {
            EditIssue.ALREADY_INCLUDED -> R.string.already_period
            EditIssue.PREVIOUS_ONGOING -> R.string.previous_ongoing
            null -> R.string.storage_error
            else -> R.string.date_invalid
        }
        AlertDialog(onDismissRequest = onClear, text = { Text(stringResource(message)) }, confirmButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.done)) } })
    }
    editor?.let { DayLogSheet(it, state.settings, state.prediction, editorActions) }
}

@Composable private fun Placeholder(title: Int) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.placeholder))
    }
}
@Composable private fun RootPreview(dark: Boolean) {
    val today = LocalDate.of(2026, 10, 24)
    LunatideTheme(dark) { LunatideRoot(TodayUiState(ready = true, settings = AppSettings(onboardingCompleted = true), today = today, prediction = predict(emptyList(), today)), ActionState(), OnboardingState(), { _, _, _ -> }, { _, _, _ -> }, {}, {}, {}, {}) }
}
@Preview(locale = "zh") @Composable private fun LightPreview() { RootPreview(false) }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { RootPreview(true) }
