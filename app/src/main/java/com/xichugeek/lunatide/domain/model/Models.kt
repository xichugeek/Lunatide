package com.xichugeek.lunatide.domain.model

import java.time.LocalDate

data class Period(val id: Long = 0, val start: LocalDate, val end: LocalDate?, val createdAt: Long = 0, val updatedAt: Long = 0) {
    fun effectiveEnd(today: LocalDate): LocalDate = end ?: minOf(today, start.plusDays(14))
    fun contains(day: LocalDate, today: LocalDate): Boolean = day >= start && day <= effectiveEnd(today)
    val length: Int? get() = end?.let { (it.toEpochDay() - start.toEpochDay() + 1).toInt() }
}
enum class Flow { SPOTTING, LIGHT, MEDIUM, HEAVY, VERY_HEAVY }
enum class Mood { HAPPY, CALM, LOW, IRRITABLE, ANXIOUS, SENSITIVE }
enum class Symptom {
    CRAMPS, BACK_PAIN, HEADACHE, BREAST_TENDERNESS, BLOATING, ACNE, FATIGUE, NAUSEA,
    DIARRHEA, CONSTIPATION, INSOMNIA, CRAVINGS, DIZZINESS, DISCHARGE, COLD_HANDS_FEET,
}
enum class Intercourse { PROTECTED, UNPROTECTED }
data class DayLog(
    val day: LocalDate, val flow: Flow? = null, val pain: Int? = null, val mood: Mood? = null,
    val symptoms: Set<Symptom> = emptySet(), val bbtCentiCelsius: Int? = null,
    val intercourse: Intercourse? = null, val note: String? = null, val updatedAt: Long = 0,
) {
    val isEmpty get() = flow == null && pain == null && mood == null && symptoms.isEmpty() &&
        bbtCentiCelsius == null && intercourse == null && note.isNullOrBlank()
    fun valid(today: LocalDate) = day <= today && (pain == null || pain in 0..3) &&
        (bbtCentiCelsius == null || bbtCentiCelsius in 3400..4200) && (note?.length ?: 0) <= 500
}

data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(day: LocalDate): Boolean = day >= start && day <= end
}

/** Unknown stable codes from a newer backup or database are ignored. */
inline fun <reified T : Enum<T>> enumOrNull(code: String?): T? = enumValues<T>().firstOrNull { it.name == code }
