package com.xichugeek.lunatide.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.data.settings.*
import com.xichugeek.lunatide.domain.reminder.ReminderKind
import com.xichugeek.lunatide.reminder.ReminderCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ReminderUiState(val settings: AppSettings = AppSettings(), val ready: Boolean = false, val failed: Boolean = false)
data class ReminderActions(
    val enabled: (ReminderKind, Boolean) -> Unit = { _, _ -> }, val time: (Int) -> Unit = {},
    val daysBefore: (Int) -> Unit = {}, val privateMode: (Boolean) -> Unit = {},
    val requestPermission: () -> Unit = {}, val systemSettings: () -> Unit = {},
)
@HiltViewModel
class ReminderViewModel @Inject constructor(private val settings: SettingsStore, coordinator: ReminderCoordinator) : ViewModel() {
    private val localFailure = MutableStateFlow(false)
    val state = combine(settings.settings, coordinator.failed, localFailure) { settings, scheduling, local -> ReminderUiState(settings, true, scheduling || local) }
        .catch { error -> if (error is CancellationException) throw error; emit(ReminderUiState(failed = true)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReminderUiState())
    val actions = ReminderActions(::enable, ::time, ::daysBefore, ::privateMode)
    private fun edit(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try { settings.update(transform); localFailure.value = false }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { localFailure.value = true }
        }
    }
    fun enable(kind: ReminderKind, enabled: Boolean) = edit { when (kind) {
        ReminderKind.UPCOMING -> it.copy(reminderUpcomingEnabled = enabled)
        ReminderKind.START -> it.copy(reminderStartEnabled = enabled)
        ReminderKind.END -> it.copy(reminderEndEnabled = enabled)
        ReminderKind.DAILY -> it.copy(reminderDailyEnabled = enabled)
        ReminderKind.FERTILE -> it.copy(reminderFertileEnabled = enabled)
    } }
    fun time(minutes: Int) = edit { it.copy(reminderTimeMinutes = minutes.coerceIn(0, 1439)) }
    fun daysBefore(days: Int) = edit { it.copy(reminderDaysBefore = days.takeIf { value -> value in setOf(1, 2, 3, 5) } ?: 2) }
    fun privateMode(enabled: Boolean) = edit { it.copy(notificationPrivateMode = enabled) }
}
