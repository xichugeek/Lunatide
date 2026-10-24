package com.xichugeek.lunatide.di

import android.content.Context
import com.xichugeek.lunatide.core.security.DatabaseKeyManager
import com.xichugeek.lunatide.core.time.*
import com.xichugeek.lunatide.data.db.*
import com.xichugeek.lunatide.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton fun clock(clock: SystemTodayProvider): TodayProvider = clock
    @Provides @Singleton fun keys(@ApplicationContext context: Context) = DatabaseKeyManager(
        File(context.noBackupFilesDir, "db_key.bin"), context.getDatabasePath("lunatide.db"),
    )
    @Provides @Singleton fun helper(): HelperFactoryProvider = encryptedHelperProvider()
    @Provides fun cycles(records: RecordsRepository): CycleRepository = records
    @Provides fun logs(records: RecordsRepository): DayLogRepository = records
}
