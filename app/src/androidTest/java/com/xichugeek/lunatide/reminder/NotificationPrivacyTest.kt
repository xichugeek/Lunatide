package com.xichugeek.lunatide.reminder

import android.app.Notification
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.reminder.ReminderKind
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPrivacyTest {
    @Test fun privateAndPublicTextAreNeutralInBothLanguages() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val forbidden = listOf("经期", "例假", "排卵", "易孕", "period", "ovulat", "fertil", "cycle")
        listOf(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH).forEach { locale ->
            val context = base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
            ReminderKind.entries.forEach { kind ->
                val notification = ReminderNotifications(context).build(kind, AppSettings(), 8)
                assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
                val body = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().lowercase(locale)
                val public = notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString().lowercase(locale)
                forbidden.forEach { assertFalse(body.contains(it)); assertFalse(public.contains(it)) }
                val detailed = ReminderNotifications(context).build(kind, AppSettings(notificationPrivateMode = false), 8)
                val detailedPublic = detailed.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString().lowercase(locale)
                forbidden.forEach { assertFalse(detailedPublic.contains(it)) }
            }
        }
    }
    @Test fun workManagerUsesApplicationConfiguration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(WorkManager.getInstance(context))
    }
}
