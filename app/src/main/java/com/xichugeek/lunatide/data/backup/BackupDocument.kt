package com.xichugeek.lunatide.data.backup

import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.model.*
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class BackupPeriod(val start: String, val end: String? = null, val updatedAt: Long = 0)
@Serializable data class BackupDayLog(
    val day: String, val flow: String? = null, val pain: Int? = null, val mood: String? = null,
    val symptoms: List<String> = emptyList(), val bbtCentiCelsius: Int? = null, val intercourse: String? = null,
    val note: String? = null, val updatedAt: Long = 0,
)
/** An explicit whitelist prevents access-control settings or keys entering a portable backup. */
@Serializable data class BackupSettings(
    val initialCycleLength: Int? = null, val initialPeriodLength: Int? = null,
    val manualCycleLength: Int? = null, val manualPeriodLength: Int? = null,
    val showFertility: Boolean = true, val showBbt: Boolean = true, val showIntercourse: Boolean = false,
    val weekStart: String = "MONDAY", val theme: String = "SYSTEM", val reminderTimeMinutes: Int = 1200,
    val reminderUpcomingEnabled: Boolean = true, val reminderDaysBefore: Int = 2, val reminderStartEnabled: Boolean = true,
    val reminderEndEnabled: Boolean = true, val reminderDailyEnabled: Boolean = false, val reminderFertileEnabled: Boolean = false,
    val notificationPrivateMode: Boolean = true,
) {
    fun applyTo(old: AppSettings): AppSettings = old.copy(onboardingCompleted = true,
        initialCycleLength = initialCycleLength?.coerceIn(15, 90), initialPeriodLength = initialPeriodLength?.coerceIn(1, 15),
        manualCycleLength = manualCycleLength?.coerceIn(15, 90), manualPeriodLength = manualPeriodLength?.coerceIn(1, 15),
        showFertility = showFertility, showBbt = showBbt, showIntercourse = showIntercourse,
        weekStart = weekStart.takeIf { it in listOf("MONDAY", "SUNDAY") } ?: old.weekStart,
        theme = theme.takeIf { it in listOf("SYSTEM", "LIGHT", "DARK") } ?: old.theme,
        reminderTimeMinutes = reminderTimeMinutes.coerceIn(0, 1439), reminderUpcomingEnabled = reminderUpcomingEnabled,
        reminderDaysBefore = reminderDaysBefore.takeIf { it in listOf(1, 2, 3, 5) } ?: 2,
        reminderStartEnabled = reminderStartEnabled, reminderEndEnabled = reminderEndEnabled,
        reminderDailyEnabled = reminderDailyEnabled, reminderFertileEnabled = reminderFertileEnabled, notificationPrivateMode = notificationPrivateMode)
    companion object {
        fun from(s: AppSettings) = BackupSettings(s.initialCycleLength, s.initialPeriodLength, s.manualCycleLength, s.manualPeriodLength,
            s.showFertility, s.showBbt, s.showIntercourse, s.weekStart, s.theme, s.reminderTimeMinutes, s.reminderUpcomingEnabled,
            s.reminderDaysBefore, s.reminderStartEnabled, s.reminderEndEnabled, s.reminderDailyEnabled, s.reminderFertileEnabled, s.notificationPrivateMode)
    }
}
@Serializable data class BackupDocument(
    val format: String = "lunatide-backup", val formatVersion: Int = 1, val exportedAt: String,
    val appVersionName: String, val appVersionCode: Int,
    val periods: List<BackupPeriod> = emptyList(), val dayLogs: List<BackupDayLog> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
) {
    fun encode(): ByteArray = json.encodeToString(this).toByteArray(Charsets.UTF_8)
    fun records(today: LocalDate): Pair<List<Period>, List<DayLog>> {
        try {
            require(format == "lunatide-backup" && formatVersion == 1)
            OffsetDateTime.parse(exportedAt)
            require(periods.size <= 20_000 && dayLogs.size <= 100_000)
            val periods = periods.map { p -> Period(start = LocalDate.parse(p.start), end = p.end?.let(LocalDate::parse), updatedAt = p.updatedAt).also {
                require(it.start <= today && (it.end == null || it.end in it.start..today))
            } }
            val logs = dayLogs.map { log -> DayLog(LocalDate.parse(log.day), enumOrNull<Flow>(log.flow), log.pain, enumOrNull<Mood>(log.mood),
                log.symptoms.mapNotNull { enumOrNull<Symptom>(it) }.toSet(), log.bbtCentiCelsius, enumOrNull<Intercourse>(log.intercourse), log.note, log.updatedAt)
                .also { require(it.valid(today)) } }
            return periods to logs
        } catch (_: Exception) { throw BackupException(BackupError.INVALID_DATA) }
    }
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun decode(bytes: ByteArray): BackupDocument = try { json.decodeFromString<BackupDocument>(bytes.toString(Charsets.UTF_8)) }
            catch (_: Exception) { throw BackupException(BackupError.INVALID_DATA) }
        fun from(periods: List<Period>, logs: List<DayLog>, settings: AppSettings, exportedAt: String, name: String, code: Int) = BackupDocument(
            exportedAt = exportedAt, appVersionName = name, appVersionCode = code,
            periods = periods.map { BackupPeriod(it.start.toString(), it.end?.toString(), it.updatedAt) },
            dayLogs = logs.map { BackupDayLog(it.day.toString(), it.flow?.name, it.pain, it.mood?.name, it.symptoms.map(Symptom::name).sorted(), it.bbtCentiCelsius, it.intercourse?.name, it.note, it.updatedAt) },
            settings = BackupSettings.from(settings))
    }
}
