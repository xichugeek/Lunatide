package com.xichugeek.lunatide.data.repository

import androidx.room.withTransaction
import com.xichugeek.lunatide.core.security.DataUnavailable
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.db.*
import com.xichugeek.lunatide.data.db.mappers.*
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow as DataFlow

interface CycleRepository {
    val periods: DataFlow<List<Period>>
    suspend fun edit(action: PeriodAction, day: LocalDate, recent: Boolean = false, shorten: Boolean = false): Result<PeriodEdit>
}
interface DayLogRepository {
    val logs: DataFlow<List<DayLog>>
    suspend fun save(log: DayLog): Result<Unit>
}

@Singleton
class RecordsRepository @Inject constructor(private val owner: DatabaseOwner, private val clock: TodayProvider) : CycleRepository, DayLogRepository {
    override val periods = flow { emitAll(owner.get().records().periods().map { rows -> rows.map { it.toModel() } }) }
        .catch { if (it is CancellationException) throw it; throw DataUnavailable(it) }.flowOn(Dispatchers.IO)
    override val logs = flow { emitAll(owner.get().records().logs().map { rows -> rows.map { it.toModel() } }) }
        .catch { if (it is CancellationException) throw it; throw DataUnavailable(it) }.flowOn(Dispatchers.IO)

    override suspend fun edit(action: PeriodAction, day: LocalDate, recent: Boolean, shorten: Boolean): Result<PeriodEdit> = storageResult {
        val db = owner.get()
        db.withTransaction {
            val old = db.records().periodSnapshot().map { it.toModel() }
            val result = PeriodRules.edit(old, action, day, clock.today.value, recent, shorten)
            if (result is PeriodEdit.Applied) {
                check(PeriodRules.valid(result.periods, clock.today.value))
                val now = clock.timestamp()
                val updated = result.periods.map { p ->
                    if (p in old) p else p.copy(createdAt = if (p.createdAt == 0L) now else p.createdAt, updatedAt = now)
                }
                db.records().deletePeriods()
                db.records().insertPeriods(updated.map { it.toEntity() })
                result.clearFlow?.let { db.records().clearFlow(it.start.toEpochDay(), it.end.toEpochDay(), now) }
                removeEmptyLogs(db, result.periods)
            }
            result
        }
    }

    override suspend fun save(log: DayLog): Result<Unit> = storageResult {
        require(log.valid(clock.today.value))
        val db = owner.get()
        db.withTransaction {
            val inPeriod = db.records().periodSnapshot().any { it.toModel().contains(log.day, clock.today.value) }
            require(log.flow == null || log.flow == Flow.SPOTTING || inPeriod)
            if (log.isEmpty && !inPeriod) db.records().deleteLog(log.day.toEpochDay())
            else {
                db.records().upsert(log.copy(updatedAt = clock.timestamp()).toEntity())
                db.records().deleteSymptoms(log.day.toEpochDay())
                db.records().insertSymptoms(log.symptoms.map { DaySymptomEntity(log.day.toEpochDay(), it.name) })
            }
        }
    }

    private suspend fun removeEmptyLogs(db: LunatideDatabase, periods: List<Period>) {
        db.records().logSnapshot().forEach {
            val log = it.toModel()
            if (log.isEmpty && periods.none { p -> p.contains(log.day, clock.today.value) }) db.records().deleteLog(it.log.day)
        }
    }

    private suspend fun <T> storageResult(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try { Result.success(block()) }
        catch (error: CancellationException) { throw error }
        catch (error: IllegalArgumentException) { Result.failure(error) }
        catch (error: Exception) { Result.failure(DataUnavailable(error)) }
    }
}
