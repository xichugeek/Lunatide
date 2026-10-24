package com.xichugeek.lunatide.data.settings

import com.xichugeek.lunatide.domain.prediction.PredictionConfig
import com.xichugeek.lunatide.domain.reminder.*

fun AppSettings.toPredictionConfig() = PredictionConfig(manualCycleLength, manualPeriodLength, initialCycleLength, initialPeriodLength, showFertility = showFertility)
fun AppSettings.toReminderConfig() = ReminderConfig(buildSet {
    if (reminderUpcomingEnabled) add(ReminderKind.UPCOMING)
    if (reminderStartEnabled) add(ReminderKind.START)
    if (reminderEndEnabled) add(ReminderKind.END)
    if (reminderDailyEnabled) add(ReminderKind.DAILY)
    if (reminderFertileEnabled) add(ReminderKind.FERTILE)
}, reminderTimeMinutes, reminderDaysBefore, toPredictionConfig())
