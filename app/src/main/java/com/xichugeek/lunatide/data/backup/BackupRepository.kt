package com.xichugeek.lunatide.data.backup

import androidx.room.withTransaction
import com.xichugeek.lunatide.BuildConfig
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.db.*
import com.xichugeek.lunatide.data.db.mappers.*
import com.xichugeek.lunatide.data.settings.SettingsStore
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.PeriodRules
import com.xichugeek.lunatide.reminder.ReminderCoordinator
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class BackupRepository @Inject constructor(
    private val owner: DatabaseOwner, private val settings: SettingsStore, private val clock: TodayProvider,
    private val recovery: DatabaseRecovery, private val reminders: ReminderCoordinator,
) {
    private val mutex = Mutex()
    suspend fun snapshot(): BackupDocument = withContext(Dispatchers.IO) {
        val s = settings.settings.first()
        owner.get().withTransaction {
            val dao = owner.get().records()
            BackupDocument.from(dao.periodSnapshot().map { it.toModel() }, dao.logSnapshot().map { it.toModel() }, s,
                Instant.ofEpochMilli(clock.timestamp()).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString(), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        }
    }
    suspend fun restore(document: BackupDocument, merge: Boolean, replaceInaccessible: Boolean = false): ImportResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val today = clock.today.value
            val incoming = document.records(today)
            require(!replaceInaccessible || !merge)
            val oldSettings = settings.settings.first()
            var result = ImportResult(0, 0, 0)
            suspend fun write(db: LunatideDatabase) = db.withTransaction {
                val dao = db.records()
                val oldPeriods = if (merge) dao.periodSnapshot().map { it.toModel() } else emptyList()
                val oldLogs = if (merge) dao.logSnapshot().map { it.toModel() } else emptyList()
                val normalized = mergeRecords(oldPeriods, oldLogs, incoming.first, incoming.second, today)
                require(PeriodRules.valid(normalized.periods, today))
                dao.deleteLogs(); dao.deletePeriods()
                dao.insertPeriods(normalized.periods.map { it.toEntity() })
                normalized.logs.forEach { log ->
                    dao.upsert(log.toEntity())
                    dao.insertSymptoms(log.symptoms.map { DaySymptomEntity(log.day.toEpochDay(), it.name) })
                }
                if (!merge) settings.update { document.settings.applyTo(it) }
                result = normalized.result
            }
            try {
                if (replaceInaccessible) { reminders.stop(); recovery.replace(::write) }
                else write(owner.get())
                result
            } catch (error: Exception) {
                if (!merge) withContext(NonCancellable) { settings.update { oldSettings } }
                throw error
            }
        }
    }
}
