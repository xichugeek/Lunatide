package com.xichugeek.lunatide.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.settingsStore by preferencesDataStore("settings")
@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false, val initialCycleLength: Int? = null,
    val initialPeriodLength: Int? = null, val manualCycleLength: Int? = null, val manualPeriodLength: Int? = null,
    val showFertility: Boolean = true, val showBbt: Boolean = true, val showIntercourse: Boolean = false,
    val weekStart: String = "MONDAY", val theme: String = "SYSTEM",
    val reminderTimeMinutes: Int = 1200, val reminderUpcomingEnabled: Boolean = true, val reminderDaysBefore: Int = 2,
    val reminderStartEnabled: Boolean = true, val reminderEndEnabled: Boolean = true,
    val reminderDailyEnabled: Boolean = false, val reminderFertileEnabled: Boolean = false,
    val notificationPrivateMode: Boolean = true, val secureScreen: Boolean = false,
    val lockEnabled: Boolean = false, val lockBiometric: Boolean = false, val lockTimeoutSeconds: Int = 0,
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) : SettingsStore {
    private val store = context.settingsStore
    override val settings = store.data.map(::decode)
    override suspend fun update(transform: (AppSettings) -> AppSettings) { store.edit { encode(transform(decode(it)), it) } }
    override suspend fun clear() { store.edit { it.clear() } }

    private fun decode(p: Preferences) = AppSettings(
        p[booleanPreferencesKey("onboarding_completed")] ?: false,
        p[intPreferencesKey("initial_cycle_length")], p[intPreferencesKey("initial_period_length")],
        p[intPreferencesKey("manual_cycle_length")], p[intPreferencesKey("manual_period_length")],
        p[booleanPreferencesKey("show_fertility")] ?: true, p[booleanPreferencesKey("show_bbt")] ?: true,
        p[booleanPreferencesKey("show_intercourse")] ?: false, p[stringPreferencesKey("week_start")] ?: "MONDAY",
        p[stringPreferencesKey("theme")] ?: "SYSTEM", p[intPreferencesKey("reminder_time_minutes")] ?: 1200,
        p[booleanPreferencesKey("reminder_upcoming_enabled")] ?: true, p[intPreferencesKey("reminder_upcoming_days_before")] ?: 2,
        p[booleanPreferencesKey("reminder_start_enabled")] ?: true, p[booleanPreferencesKey("reminder_end_enabled")] ?: true,
        p[booleanPreferencesKey("reminder_daily_enabled")] ?: false, p[booleanPreferencesKey("reminder_fertile_enabled")] ?: false,
        p[booleanPreferencesKey("notification_private_mode")] ?: true, p[booleanPreferencesKey("secure_screen")] ?: false,
        p[booleanPreferencesKey("lock_enabled")] ?: false, p[booleanPreferencesKey("lock_biometric")] ?: false,
        p[intPreferencesKey("lock_timeout_seconds")] ?: 0,
    )
    private fun encode(s: AppSettings, p: MutablePreferences) {
        fun bool(key: String, value: Boolean) { p[booleanPreferencesKey(key)] = value }
        fun int(key: String, value: Int?) { val k = intPreferencesKey(key); if (value == null) p.remove(k) else p[k] = value }
        bool("onboarding_completed", s.onboardingCompleted)
        int("initial_cycle_length", s.initialCycleLength); int("initial_period_length", s.initialPeriodLength)
        int("manual_cycle_length", s.manualCycleLength); int("manual_period_length", s.manualPeriodLength)
        bool("show_fertility", s.showFertility); bool("show_bbt", s.showBbt); bool("show_intercourse", s.showIntercourse)
        p[stringPreferencesKey("week_start")] = s.weekStart; p[stringPreferencesKey("theme")] = s.theme
        int("reminder_time_minutes", s.reminderTimeMinutes); int("reminder_upcoming_days_before", s.reminderDaysBefore)
        bool("reminder_upcoming_enabled", s.reminderUpcomingEnabled); bool("reminder_start_enabled", s.reminderStartEnabled)
        bool("reminder_end_enabled", s.reminderEndEnabled); bool("reminder_daily_enabled", s.reminderDailyEnabled)
        bool("reminder_fertile_enabled", s.reminderFertileEnabled); bool("notification_private_mode", s.notificationPrivateMode)
        bool("secure_screen", s.secureScreen); bool("lock_enabled", s.lockEnabled); bool("lock_biometric", s.lockBiometric)
        int("lock_timeout_seconds", s.lockTimeoutSeconds)
    }
}

interface SettingsStore {
    val settings: kotlinx.coroutines.flow.Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun clear()
}
