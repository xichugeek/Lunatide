package com.xichugeek.lunatide.data.db.mappers

import com.xichugeek.lunatide.data.db.*
import com.xichugeek.lunatide.domain.model.*
import java.time.LocalDate

fun PeriodEntity.toModel() = Period(id, LocalDate.ofEpochDay(startDay), endDay?.let(LocalDate::ofEpochDay), createdAt, updatedAt)
fun Period.toEntity() = PeriodEntity(id, start.toEpochDay(), end?.toEpochDay(), createdAt, updatedAt)
fun DayWithSymptoms.toModel() = DayLog(
    LocalDate.ofEpochDay(log.day), enumOrNull(log.flow), log.pain, enumOrNull(log.mood),
    symptoms.mapNotNull { enumOrNull<Symptom>(it.symptom) }.toSet(), log.bbtCentiCelsius,
    enumOrNull(log.intercourse), log.note, log.updatedAt,
)
fun DayLog.toEntity() = DayLogEntity(day.toEpochDay(), flow?.name, pain, mood?.name, bbtCentiCelsius, intercourse?.name, note, updatedAt)
