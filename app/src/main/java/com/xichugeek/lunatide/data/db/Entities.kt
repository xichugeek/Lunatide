package com.xichugeek.lunatide.data.db

import androidx.room.*

@Entity(tableName = "periods", indices = [Index(value = ["start_day"], unique = true)])
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_day") val startDay: Long,
    @ColumnInfo(name = "end_day") val endDay: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
@Entity(tableName = "day_logs")
data class DayLogEntity(
    @PrimaryKey val day: Long, val flow: String?, val pain: Int?, val mood: String?,
    @ColumnInfo(name = "bbt_centi_celsius") val bbtCentiCelsius: Int?,
    val intercourse: String?, val note: String?, @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
@Entity(tableName = "day_symptoms", primaryKeys = ["day", "symptom"], foreignKeys = [
    ForeignKey(entity = DayLogEntity::class, parentColumns = ["day"], childColumns = ["day"], onDelete = ForeignKey.CASCADE),
])
data class DaySymptomEntity(val day: Long, val symptom: String)

data class DayWithSymptoms(
    @Embedded val log: DayLogEntity,
    @Relation(parentColumn = "day", entityColumn = "day") val symptoms: List<DaySymptomEntity>,
)
