package com.xichugeek.lunatide.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordsDao {
    @Query("SELECT * FROM periods ORDER BY start_day") fun periods(): Flow<List<PeriodEntity>>
    @Query("SELECT * FROM periods ORDER BY start_day") suspend fun periodSnapshot(): List<PeriodEntity>
    @Insert suspend fun insertPeriods(periods: List<PeriodEntity>)
    @Query("DELETE FROM periods") suspend fun deletePeriods()
    @Transaction @Query("SELECT * FROM day_logs ORDER BY day") fun logs(): Flow<List<DayWithSymptoms>>
    @Transaction @Query("SELECT * FROM day_logs ORDER BY day") suspend fun logSnapshot(): List<DayWithSymptoms>
    @Upsert suspend fun upsert(log: DayLogEntity)
    @Insert suspend fun insertSymptoms(symptoms: List<DaySymptomEntity>)
    @Query("DELETE FROM day_symptoms WHERE day = :day") suspend fun deleteSymptoms(day: Long)
    @Query("DELETE FROM day_logs WHERE day = :day") suspend fun deleteLog(day: Long)
    @Query("DELETE FROM day_logs") suspend fun deleteLogs()
    @Query("UPDATE day_logs SET flow = NULL, updated_at = :updatedAt WHERE day BETWEEN :start AND :end AND flow != 'SPOTTING'")
    suspend fun clearFlow(start: Long, end: Long, updatedAt: Long)
}

@Database(entities = [PeriodEntity::class, DayLogEntity::class, DaySymptomEntity::class], version = 1, exportSchema = true)
abstract class LunatideDatabase : RoomDatabase() {
    abstract fun records(): RecordsDao
}
