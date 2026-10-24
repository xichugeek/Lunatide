package com.xichugeek.lunatide

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration
import com.xichugeek.lunatide.reminder.*
import javax.inject.Inject

@HiltAndroidApp
class LunatideApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: ReminderWorkerFactory
    @Inject lateinit var reminders: ReminderCoordinator
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).setMinimumLoggingLevel(android.util.Log.ERROR).build()
    override fun onCreate() { super.onCreate(); reminders.start() }
}
