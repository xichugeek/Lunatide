package com.xichugeek.lunatide.ui.data

import android.content.Context
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.data.backup.CsvLabels
import com.xichugeek.lunatide.ui.components.*

fun csvLabels(context: Context): CsvLabels {
    fun text(id: Int) = context.getString(id)
    return CsvLabels(listOf(R.string.csv_date, R.string.this_day_period, R.string.csv_period_day, R.string.flow, R.string.pain,
        R.string.symptoms, R.string.mood, R.string.temperature, R.string.intercourse, R.string.note).map(::text), text(R.string.yes), text(R.string.no),
        Flow.entries.associateWith { text(flowLabel(it)) }, listOf(R.string.pain_none, R.string.pain_mild, R.string.pain_moderate, R.string.pain_severe).mapIndexed { i, id -> i to text(id) }.toMap(),
        Symptom.entries.associateWith { text(symptomLabel(it)) }, Mood.entries.associateWith { text(moodLabel(it)) },
        mapOf(Intercourse.PROTECTED to text(R.string.protected_intercourse), Intercourse.UNPROTECTED to text(R.string.unprotected_intercourse)))
}
