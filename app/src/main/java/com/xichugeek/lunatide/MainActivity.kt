package com.xichugeek.lunatide

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.Intent
import androidx.core.net.toUri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import android.view.WindowManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.xichugeek.lunatide.core.security.*
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.ui.security.PinScreen

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val today: TodayViewModel by viewModels()
    private val onboarding: OnboardingViewModel by viewModels()
    private val calendar: CalendarViewModel by viewModels()
    private val editor: DayLogViewModel by viewModels()
    private val stats: StatsViewModel by viewModels()
    private val reminder: ReminderViewModel by viewModels()
    @Inject lateinit var reminders: ReminderCoordinator
    @Inject lateinit var appLock: AppLockController
    @Inject lateinit var clock: TodayProvider
    private val notificationsGranted = MutableStateFlow(false)
    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        appLock.externalReturned()
        notificationsGranted.value = notificationPermissionGranted(this)
        reminders.refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            val lock by appLock.state.collectAsState()
            val dark = when (lock.settings.theme) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
            LaunchedEffect(lock.ready, lock.settings.secureScreen) {
                if (!lock.ready || lock.settings.secureScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            LaunchedEffect(lock.erased) { if (lock.erased) {
                appLock.acknowledgeErase(); reminders.start()
                startActivity(Intent(this@MainActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish()
            } }
            LunatideTheme(dark) {
                if (!lock.ready && lock.error == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (lock.locked || lock.mode != PinMode.NONE || !lock.ready) {
                    PinScreen(lock, appLock::digit, appLock::submit, appLock::dismiss,
                        { showBiometric(this@MainActivity, appLock::biometricSuccess, appLock::biometricError) }, appLock::erase)
                } else {
                    val state by today.state.collectAsState()
                    val actions by today.actions.collectAsState()
                    val onboardingState by onboarding.state.collectAsState()
                    val calendarState by calendar.state.collectAsState()
                    val editorState by editor.state.collectAsState()
                    val statsState by stats.state.collectAsState()
                    val reminderState by reminder.state.collectAsState()
                    val granted by notificationsGranted.collectAsState()
                    LunatideRoot(state, actions, onboardingState, onboarding::finish,
                        { date, recent, revise -> today.record(date, recent, revise) }, today::undo, today::spotting, today::clearMessage, today::retry,
                        calendarState, calendar::move, calendar::today, editorState, editor.actions, editor::open, statsState, calendar::show,
                        reminderState, reminder.actions.copy(requestPermission = ::requestNotifications, systemSettings = ::openSystemSettings), granted, onboarding::draft,
                        lock, biometricAuthenticators(this@MainActivity) != null, appLock::begin, appLock::update)
                }
            }
        }
    }
    override fun onStart() { super.onStart(); appLock.foreground() }
    override fun onStop() { if (!isChangingConfigurations) appLock.background(); super.onStop() }
    override fun onResume() {
        super.onResume(); clock.refresh(); appLock.foreground()
        notificationsGranted.value = notificationPermissionGranted(this)
        reminders.refresh()
    }
    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) { appLock.externalJump(); notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS) }
        else openSystemSettings()
    }
    private fun openSystemSettings() { appLock.externalJump(); startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())) }
}
