package com.xichugeek.lunatide.data

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xichugeek.lunatide.core.security.*
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.db.*
import com.xichugeek.lunatide.data.repository.RecordsRepository
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val testDir = File(base.noBackupFilesDir, "instrumented-cipher-test")
    private val context = object : ContextWrapper(base) {
        override fun getDatabasePath(name: String) = File(testDir, name)
    }
    private lateinit var keys: DatabaseKeyManager
    private lateinit var owner: DatabaseOwner
    private val day = LocalDate.of(2026, 10, 24)
    private val clock = object : TodayProvider {
        override val today = MutableStateFlow(day)
        override fun refresh() = Unit
        override fun timestamp() = 1234L
    }
    @Before fun setup() {
        testDir.mkdirs()
        keys = DatabaseKeyManager(File(testDir, "db_key.bin"), context.getDatabasePath("lunatide.db"), "lunatide_instrumentation_kek")
        owner = DatabaseOwner(context, keys, encryptedHelperProvider())
    }
    @After fun cleanup() { owner.close(); keys.delete(); testDir.deleteRecursively() }

    @Test fun encryptedRoundTripReopenAndWrongPassword() = runBlocking(Dispatchers.IO) {
        val records = RecordsRepository(owner, clock)
        assertTrue(records.edit(PeriodAction.START, day).getOrThrow() is PeriodEdit.Applied)
        records.save(DayLog(day, flow = com.xichugeek.lunatide.domain.model.Flow.MEDIUM, symptoms = setOf(Symptom.FATIGUE), note = "Fictional example")).getOrThrow()
        owner.close()
        assertEquals("Fictional example", records.logs.first().single().note)
        assertEquals(Symptom.FATIGUE, records.logs.first().single().symptoms.single())
        owner.close()
        assertFalse(context.getDatabasePath("lunatide.db").inputStream().use { stream ->
            val header = ByteArray(16); java.io.DataInputStream(stream).readFully(header)
            String(header, Charsets.ISO_8859_1)
        }.startsWith("SQLite format 3"))
        val wrong = Room.databaseBuilder(context, LunatideDatabase::class.java, "lunatide.db")
            .openHelperFactory(encryptedHelperProvider().create(ByteArray(32) { 9 })).build()
        try { wrong.openHelper.writableDatabase.query("SELECT 1").close(); fail("Wrong password accepted") }
        catch (_: android.database.sqlite.SQLiteException) { /* Expected authentication failure. */ }
        finally { wrong.close() }
        assertEquals(32, keys.password().also { it.fill(0) }.size)
    }
    @Test fun lostKeystoreDoesNotDeleteExistingDatabase() = runBlocking(Dispatchers.IO) {
        owner.get().records().insertPeriods(listOf(PeriodEntity(startDay = day.toEpochDay(), endDay = null, createdAt = 1, updatedAt = 1)))
        owner.close()
        val dbFile = context.getDatabasePath("lunatide.db")
        val oldLength = dbFile.length()
        java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("lunatide_instrumentation_kek") }
        try { owner.get(); fail("Lost key accepted") } catch (_: DataUnavailable) { }
        assertTrue(dbFile.exists()); assertEquals(oldLength, dbFile.length())
    }
    @Test fun missingWrappedKeyDoesNotGenerateReplacementForExistingDatabase() = runBlocking(Dispatchers.IO) {
        owner.get(); owner.close(); File(testDir, "db_key.bin").delete()
        try { owner.get(); fail("Missing wrapped key accepted") } catch (_: DataUnavailable) { }
        assertFalse(File(testDir, "db_key.bin").exists()); assertTrue(context.getDatabasePath("lunatide.db").exists())
    }
    @Test fun shorteningClearsFlowButKeepsSpottingAndOtherFields() = runBlocking(Dispatchers.IO) {
        val repo = RecordsRepository(owner, clock)
        repo.edit(PeriodAction.START, day.minusDays(3)).getOrThrow()
        repo.edit(PeriodAction.END, day).getOrThrow()
        repo.save(DayLog(day.minusDays(1), flow = com.xichugeek.lunatide.domain.model.Flow.MEDIUM, note = "Example")).getOrThrow()
        repo.save(DayLog(day, flow = com.xichugeek.lunatide.domain.model.Flow.SPOTTING)).getOrThrow()
        repo.edit(PeriodAction.EXCLUDE, day.minusDays(1), shorten = true).getOrThrow()
        val logs = repo.logs.first()
        assertNull(logs.first { it.day == day.minusDays(1) }.flow)
        assertEquals("Example", logs.first { it.day == day.minusDays(1) }.note)
        assertEquals(com.xichugeek.lunatide.domain.model.Flow.SPOTTING, logs.first { it.day == day }.flow)
    }
    @Test fun unencryptedRoomInMemoryAndTransactionRollback() = runBlocking(Dispatchers.IO) {
        val db = Room.inMemoryDatabaseBuilder(base, LunatideDatabase::class.java).build()
        try {
            try { db.withTransaction { db.records().upsert(DayLogEntity(1, null, null, null, null, null, null, 1)); error("rollback") } }
            catch (_: IllegalStateException) { }
            assertTrue(db.records().logSnapshot().isEmpty())
            db.records().upsert(DayLogEntity(1, null, null, null, null, null, null, 1))
            db.records().insertSymptoms(listOf(DaySymptomEntity(1, "FATIGUE")))
            db.records().deleteLog(1)
            assertTrue(db.records().logSnapshot().isEmpty())
        } finally { db.close() }
    }
    @Test fun undoStartPreservesNoteAndClearsIncompatibleFlow() = runBlocking(Dispatchers.IO) {
        val repo = RecordsRepository(owner, clock)
        repo.edit(PeriodAction.START, day).getOrThrow()
        repo.save(DayLog(day, flow = com.xichugeek.lunatide.domain.model.Flow.MEDIUM, note = "Fictional note")).getOrThrow()
        repo.restorePeriods(emptyList()).getOrThrow()
        val log = repo.logs.first().single()
        assertEquals("Fictional note", log.note); assertNull(log.flow); assertTrue(repo.periods.first().isEmpty())
    }
    @Test fun flowAndPeriodAreAtomicIncludingRollback() = runBlocking(Dispatchers.IO) {
        val repo = RecordsRepository(owner, clock)
        val invalid = DayLog(day, flow = com.xichugeek.lunatide.domain.model.Flow.MEDIUM, note = "x".repeat(501))
        assertTrue(repo.editDay(invalid, true).isFailure)
        assertTrue(repo.periods.first().isEmpty()); assertTrue(repo.logs.first().isEmpty())
        repo.editDay(invalid.copy(note = "Fictional"), true).getOrThrow()
        assertEquals(1, repo.periods.first().size)
        assertEquals(com.xichugeek.lunatide.domain.model.Flow.MEDIUM, repo.logs.first().single().flow)
    }
}
