package com.xichugeek.lunatide.reminder

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xichugeek.lunatide.MainActivity
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.reminder.ReminderKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    fun build(kind: ReminderKind, settings: AppSettings, ongoingDays: Int = 0): Notification {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.reminders), NotificationManager.IMPORTANCE_DEFAULT))
        val title = context.getString(R.string.app_name)
        val public = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_today)
            .setContentTitle(title).setContentText(context.getString(R.string.notification_public)).build()
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val body = if (settings.notificationPrivateMode) context.getString(when (kind) {
            ReminderKind.UPCOMING -> R.string.private_upcoming; ReminderKind.START -> R.string.private_start
            ReminderKind.END -> R.string.private_end; ReminderKind.DAILY -> R.string.private_daily; ReminderKind.FERTILE -> R.string.private_fertile
        }) else when (kind) {
            ReminderKind.UPCOMING -> context.resources.getQuantityString(R.plurals.detailed_upcoming, settings.reminderDaysBefore, settings.reminderDaysBefore)
            ReminderKind.START -> context.getString(R.string.detailed_start)
            ReminderKind.END -> context.resources.getQuantityString(R.plurals.detailed_end, ongoingDays, ongoingDays)
            ReminderKind.DAILY -> context.getString(R.string.private_daily)
            ReminderKind.FERTILE -> context.getString(R.string.detailed_fertile)
        }
        return NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_today).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public).setContentIntent(intent).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
    }
    @Suppress("MissingPermission")
    fun post(kind: ReminderKind, settings: AppSettings, ongoingDays: Int) {
        if (notificationPermissionGranted(context)) NotificationManagerCompat.from(context).notify(kind.ordinal + 100, build(kind, settings, ongoingDays))
    }
    companion object { const val CHANNEL = "reminders" }
}
