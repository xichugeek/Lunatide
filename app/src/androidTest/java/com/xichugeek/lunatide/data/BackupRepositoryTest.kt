package com.xichugeek.lunatide.data

import android.content.Context
import android.content.ContextWrapper
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.xichugeek.lunatide.LunatideApp
import com.xichugeek.lunatide.core.security.*
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.backup.*
import com.xichugeek.lunatide.data.db.*
import com.xichugeek.lunatide.data.settings.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*

class BackupRepositoryTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val directory = File(base.noBackupFilesDir, "backup-instrumentation")
    private val context = object : ContextWrapper(base) {
        override fun getDatabasePath(name: String) = File(directory, name)
        override fun getNoBackupFilesDir() = directory
        override fun deleteDatabase(name: String): Boolean {
            val db = getDatabasePath(name)
            listOf("-wal", "-shm", "-journal").forEach { File(db.path + it).delete() }
            return db.delete()
        }
    }
    private val today = LocalDate.of(2026, 10, 24)
    private val clock = object : TodayProvider {
        override val today = MutableStateFlow(this@BackupRepositoryTest.today)
        override fun refresh() = Unit
        override fun timestamp() = 1_790_000_000_000L
    }
    private val settings = object : SettingsStore {
        val flow = MutableStateFlow(AppSettings(lockEnabled = true, secureScreen = true))
        var fail = false
        override val settings = flow.asStateFlow()
        override suspend fun update(transform: (AppSettings) -> AppSettings) { if (fail) { fail = false; error("Simulated storage failure") }; flow.value = transform(flow.value) }
        override suspend fun clear() { flow.value = AppSettings() }
    }
    private lateinit var owner: DatabaseOwner
    private lateinit var keys: DatabaseKeyManager
    private lateinit var recovery: DatabaseRecovery
    private lateinit var backups: BackupRepository
    private fun document(note: String = "Fictional backup") = BackupDocument(exportedAt = "2026-10-24T12:00:00+08:00", appVersionName = "0.1.0", appVersionCode = 100,
        periods = listOf(BackupPeriod(today.minusDays(3).toString(), today.toString(), 5)), dayLogs = listOf(BackupDayLog(today.toString(), flow = "MEDIUM", note = note, updatedAt = 6)))
    @Before fun setup() {
        directory.mkdirs()
        keys = DatabaseKeyManager(File(directory, "db_key.bin"), context.getDatabasePath("lunatide.db"), "lunatide_backup_test_kek")
        val helper = encryptedHelperProvider()
        owner = DatabaseOwner(context, keys, helper)
        recovery = DatabaseRecovery(context, owner, helper, keys)
        backups = BackupRepository(owner, settings, clock, recovery, (base as LunatideApp).reminders)
    }
    @After fun cleanup() { owner.close(); keys.delete(); directory.deleteRecursively() }
    @Test fun overwriteMergeAndRollbackAreTransactional() = runBlocking(Dispatchers.IO) {
        backups.restore(document(), false)
        assertEquals("Fictional backup", backups.snapshot().dayLogs.single().note)
        assertTrue(settings.flow.value.lockEnabled); assertTrue(settings.flow.value.secureScreen)
        val merged = backups.restore(document("Newer").copy(dayLogs = listOf(BackupDayLog(today.toString(), note = "Newer", updatedAt = 10))), true)
        assertEquals(1, merged.updatedDays); assertEquals("Newer", backups.snapshot().dayLogs.single().note)
        settings.fail = true
        try { backups.restore(document("Must roll back"), false); fail("Import should fail") } catch (_: IllegalStateException) { }
        assertEquals("Newer", backups.snapshot().dayLogs.single().note)
    }
    @Test fun failedRecoveryPreservesOriginalThenConfirmedRecoveryReopens() = runBlocking(Dispatchers.IO) {
        backups.restore(document("Original"), false); owner.close()
        val dbFile = context.getDatabasePath("lunatide.db")
        val original = dbFile.readBytes()
        java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(keys.alias) }
        try { owner.get(); fail("Lost key should fail") } catch (_: DataUnavailable) { }
        try { recovery.replace { error("Interrupted preparation") }; fail("Preparation should fail") } catch (_: IllegalStateException) { }
        assertArrayEquals(original, dbFile.readBytes())
        backups.restore(document("Recovered"), false, replaceInaccessible = true)
        owner.close()
        assertEquals("Recovered", backups.snapshot().dayLogs.single().note)
        assertFalse(dbFile.readBytes().copyOfRange(0, 16).toString(Charsets.ISO_8859_1).startsWith("SQLite format 3"))
        assertFalse(File(directory, "restore_ready").exists())
    }
    @Test fun eraseRemovesDatabaseSidecarsWrappedKeyAndStores() = runBlocking(Dispatchers.IO) {
        backups.restore(document(), false)
        val security = SecurityRepository(base)
        security.setPin(PinCrypto.create("123456".toCharArray()))
        val reset = AppDataReset(context, owner, settings, security, (base as LunatideApp).reminders)
        reset.erase()
        assertFalse(context.getDatabasePath("lunatide.db").exists())
        assertFalse(File(context.getDatabasePath("lunatide.db").path + "-wal").exists())
        assertFalse(File(context.getDatabasePath("lunatide.db").path + "-shm").exists())
        assertFalse(File(directory, "db_key.bin").exists())
        assertNull(security.records.first().digest)
        assertFalse(settings.flow.value.onboardingCompleted)
    }
}
