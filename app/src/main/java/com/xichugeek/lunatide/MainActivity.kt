package com.xichugeek.lunatide

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.Intent
import androidx.core.net.toUri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import com.xichugeek.lunatide.ui.LunatideRoot
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import com.xichugeek.lunatide.ui.today.TodayViewModel
import com.xichugeek.lunatide.ui.onboarding.OnboardingViewModel
import com.xichugeek.lunatide.ui.calendar.CalendarViewModel
import com.xichugeek.lunatide.ui.daylog.DayLogViewModel
import com.xichugeek.lunatide.ui.stats.StatsViewModel
import com.xichugeek.lunatide.ui.settings.ReminderViewModel
import com.xichugeek.lunatide.reminder.*
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val today: TodayViewModel by viewModels()
    private val onboarding: OnboardingViewModel by viewModels()
    private val calendar: CalendarViewModel by viewModels()
    private val editor: DayLogViewModel by viewModels()
    private val stats: StatsViewModel by viewModels()
    private val reminder: ReminderViewModel by viewModels()
    @Inject lateinit var reminders: ReminderCoordinator
    private val notificationsGranted = MutableStateFlow(false)
    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted.value = notificationPermissionGranted(this)
        reminders.refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by today.state.collectAsState()
            val actions by today.actions.collectAsState()
            val onboardingState by onboarding.state.collectAsState()
            val calendarState by calendar.state.collectAsState()
            val editorState by editor.state.collectAsState()
            val statsState by stats.state.collectAsState()
            val reminderState by reminder.state.collectAsState()
            val granted by notificationsGranted.collectAsState()
            val dark = when (state.settings.theme) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
            LunatideTheme(dark) {
                LunatideRoot(state, actions, onboardingState, onboarding::finish,
                    { date, recent, revise -> today.record(date, recent, revise) }, today::undo, today::spotting, today::clearMessage, today::retry,
                    calendarState, calendar::move, calendar::today, editorState, editor.actions, editor::open, statsState, calendar::show,
                    reminderState, reminder.actions.copy(requestPermission = ::requestNotifications, systemSettings = ::openSystemSettings), granted, onboarding::draft)
            }
        }
    }
    override fun onResume() {
        super.onResume(); today.refresh()
        notificationsGranted.value = notificationPermissionGranted(this)
        reminders.refresh()
    }
    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        else openSystemSettings()
    }
    private fun openSystemSettings() { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())) }
}
