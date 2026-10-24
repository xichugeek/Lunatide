package com.xichugeek.lunatide.reminder

import android.content.Context
import androidx.work.*
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.CycleRepository
import com.xichugeek.lunatide.data.settings.*
import com.xichugeek.lunatide.domain.reminder.*
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context, parameters: WorkerParameters,
    private val cycles: CycleRepository, private val settings: SettingsStore, private val clock: TodayProvider,
    private val scheduler: ReminderScheduler, private val notifications: ReminderNotifications,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        try {
            clock.refresh()
            val kind = inputData.getString(ReminderScheduler.KIND_KEY)?.let { value -> ReminderKind.entries.firstOrNull { it.name == value } }
            if (kind == null) scheduler.reschedule()
            else {
                val periods = cycles.periods.first()
                val preferences = settings.settings.first()
                if (kind in dueReminders(periods, clock.today.value, preferences.toReminderConfig()) && notificationPermissionGranted(applicationContext)) {
                    val ongoingDays = periods.lastOrNull { it.end == null }?.let { (clock.today.value.toEpochDay() - it.start.toEpochDay() + 1).toInt() } ?: 0
                    notifications.post(kind, preferences, ongoingDays)
                }
                scheduler.recomputeSoon()
            }
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { return Result.failure(workDataOf("failure_reason" to "LOCAL_DATA_UNAVAILABLE")) }
        return Result.success()
    }
}

class ReminderWorkerFactory @Inject constructor(
    private val cycles: CycleRepository, private val settings: SettingsStore, private val clock: TodayProvider,
    private val scheduler: ReminderScheduler, private val notifications: ReminderNotifications,
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == ReminderWorker::class.java.name) ReminderWorker(appContext, workerParameters, cycles, settings, clock, scheduler, notifications) else null
}
