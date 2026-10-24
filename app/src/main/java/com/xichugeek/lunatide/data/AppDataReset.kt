package com.xichugeek.lunatide.data

import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import com.xichugeek.lunatide.data.db.DatabaseOwner
import com.xichugeek.lunatide.data.settings.*
import com.xichugeek.lunatide.reminder.ReminderCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Explicit destructive operation, shared by forgotten PIN and the data settings page. */
@Singleton
class AppDataReset @Inject constructor(
    @ApplicationContext private val context: Context, private val owner: DatabaseOwner,
    private val settings: SettingsStore, private val security: SecurityRepository, private val reminders: ReminderCoordinator,
) {
    suspend fun erase() = withContext(Dispatchers.IO) {
        reminders.stop()
        WorkManager.getInstance(context).cancelAllWork().result.get()
        context.getSystemService(NotificationManager::class.java).cancelAll()
        owner.remove()
        settings.clear()
        security.clear()
    }
}
