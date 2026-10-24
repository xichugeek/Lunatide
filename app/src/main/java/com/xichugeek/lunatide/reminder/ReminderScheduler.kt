package com.xichugeek.lunatide.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.CycleRepository
import com.xichugeek.lunatide.data.settings.*
import com.xichugeek.lunatide.domain.model.Period
import com.xichugeek.lunatide.domain.reminder.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun notificationPermissionGranted(context: Context): Boolean =
    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context, private val cycles: CycleRepository,
    private val settings: SettingsStore, private val clock: TodayProvider,
) {
    private val mutex = Mutex()
    private val work get() = WorkManager.getInstance(context)
    suspend fun reschedule() = reschedule(cycles.periods.first(), settings.settings.first())
    suspend fun reschedule(periods: List<Period>, settings: AppSettings) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!notificationPermissionGranted(context)) { cancelScheduled(); return@withLock }
            val now = Instant.ofEpochMilli(clock.timestamp()).atZone(ZoneId.systemDefault())
            val config = settings.toReminderConfig()
            val plans = planReminders(periods, now, config).associateBy { it.kind }
            val due = dueReminders(periods, now.toLocalDate(), config)
            val timeTag = "lunatide_at_${config.timeMinutes}"
            ReminderKind.entries.forEach { kind ->
                val name = uniqueName(kind)
                val existing = work.getWorkInfosForUniqueWork(name).get().firstOrNull { !it.state.isFinished }
                if (existing?.state == WorkInfo.State.RUNNING) return@forEach
                if (existing != null && preserveDueJob(kind, due, Instant.ofEpochMilli(existing.nextScheduleTimeMillis), now.toInstant(), timeTag in existing.tags)) return@forEach
                val plan = plans[kind]
                if (plan == null) work.cancelUniqueWork(name)
                else {
                    val target = plan.at.toInstant().toEpochMilli()
                    if (existing != null && timeTag in existing.tags && kotlin.math.abs(existing.nextScheduleTimeMillis - target) < 1000) return@forEach
                    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInputData(workDataOf(KIND_KEY to kind.name)).addTag(timeTag)
                        .setInitialDelay(Duration.between(now.toInstant(), plan.at.toInstant()).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS).build()
                    work.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
                }
            }
            if (config.enabled.isEmpty()) work.cancelUniqueWork(DAILY_RECOMPUTE)
            else work.enqueueUniquePeriodicWork(DAILY_RECOMPUTE, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build())
        }
    }
    fun recomputeSoon() {
        work.enqueueUniqueWork(RECOMPUTE, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(1, TimeUnit.SECONDS).build())
    }
    fun cancelScheduled() {
        ReminderKind.entries.forEach { work.cancelUniqueWork(uniqueName(it)) }
        work.cancelUniqueWork(DAILY_RECOMPUTE); work.cancelUniqueWork(RECOMPUTE)
    }
    companion object {
        const val KIND_KEY = "reminder_kind"
        const val DAILY_RECOMPUTE = "lunatide_daily_recompute"
        const val RECOMPUTE = "lunatide_recompute"
        fun uniqueName(kind: ReminderKind) = "lunatide_reminder_${kind.name}"
    }
}
