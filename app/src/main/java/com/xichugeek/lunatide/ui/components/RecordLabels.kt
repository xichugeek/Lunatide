package com.xichugeek.lunatide.ui.components

import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.domain.model.*

fun flowLabel(value: Flow): Int = when (value) {
    Flow.SPOTTING -> R.string.flow_spotting; Flow.LIGHT -> R.string.flow_light
    Flow.MEDIUM -> R.string.flow_medium; Flow.HEAVY -> R.string.flow_heavy; Flow.VERY_HEAVY -> R.string.flow_very_heavy
}
fun moodLabel(value: Mood): Int = when (value) {
    Mood.HAPPY -> R.string.mood_happy; Mood.CALM -> R.string.mood_calm; Mood.LOW -> R.string.mood_low
    Mood.IRRITABLE -> R.string.mood_irritable; Mood.ANXIOUS -> R.string.mood_anxious; Mood.SENSITIVE -> R.string.mood_sensitive
}
fun symptomLabel(value: Symptom): Int = when (value) {
    Symptom.CRAMPS -> R.string.symptom_cramps; Symptom.BACK_PAIN -> R.string.symptom_back_pain
    Symptom.HEADACHE -> R.string.symptom_headache; Symptom.BREAST_TENDERNESS -> R.string.symptom_breast_tenderness
    Symptom.BLOATING -> R.string.symptom_bloating; Symptom.ACNE -> R.string.symptom_acne
    Symptom.FATIGUE -> R.string.symptom_fatigue; Symptom.NAUSEA -> R.string.symptom_nausea
    Symptom.DIARRHEA -> R.string.symptom_diarrhea; Symptom.CONSTIPATION -> R.string.symptom_constipation
    Symptom.INSOMNIA -> R.string.symptom_insomnia; Symptom.CRAVINGS -> R.string.symptom_cravings
    Symptom.DIZZINESS -> R.string.symptom_dizziness; Symptom.DISCHARGE -> R.string.symptom_discharge
    Symptom.COLD_HANDS_FEET -> R.string.symptom_cold_hands_feet
}
