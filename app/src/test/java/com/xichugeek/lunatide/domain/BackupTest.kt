package com.xichugeek.lunatide.domain

import com.xichugeek.lunatide.data.backup.*
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.model.*
import java.nio.ByteBuffer
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class BackupTest {
    private val codec = BackupCodec(1000)
    private val password = "fictional-password".toCharArray()
    private val today = LocalDate.of(2026, 10, 24)
    private fun error(expected: BackupError? = null, block: () -> Unit) {
        try { block(); fail("Expected rejected backup") }
        catch (error: BackupException) { if (expected != null) assertEquals(expected, error.reason) }
    }
    private fun document() = BackupDocument.from(listOf(Period(start = today.minusDays(30), end = today.minusDays(25), updatedAt = 7)),
        listOf(DayLog(today, mood = Mood.CALM, symptoms = setOf(Symptom.FATIGUE), note = "fictional,\"note\"\n第二行", updatedAt = 8)),
        AppSettings(lockEnabled = true, lockBiometric = true, secureScreen = true), "2026-10-24T12:00:00+08:00", "0.1.0", 100)
    @Test fun roundTripAndRandomizedCiphertext() {
        val plain = document().encode()
        val first = codec.encrypt(plain, password); val second = codec.encrypt(plain, password)
        assertArrayEquals(plain, codec.decrypt(first, password)); assertFalse(first.contentEquals(second))
        assertEquals("LUNATIDE", first.copyOfRange(0, 8).toString(Charsets.US_ASCII))
        assertEquals(document(), BackupDocument.decode(codec.decrypt(first, password)))
    }
    @Test fun wrongPasswordUsesGenericAuthenticationError() {
        val file = codec.encrypt(document().encode(), password)
        error(BackupError.AUTHENTICATION) { codec.decrypt(file, "wrong-password".toCharArray()) }
    }
    @Test fun everyHeaderAndCiphertextByteIsAuthenticated() {
        val file = codec.encrypt("{}".toByteArray(), password)
        file.indices.forEach { offset -> val changed = file.copyOf(); changed[offset] = (changed[offset].toInt() xor 1).toByte(); error { codec.decrypt(changed, password) } }
    }
    @Test fun magicAndNewerVersionAreDistinguished() {
        error(BackupError.NOT_BACKUP) { codec.decrypt("not a backup".toByteArray(), password) }
        val file = codec.encrypt(byteArrayOf(), password); file[8] = 2
        error(BackupError.NEWER_VERSION) { codec.decrypt(file, password) }
    }
    @Test fun truncatedAndHostileKdfRejectedBeforeWork() {
        val file = codec.encrypt(byteArrayOf(), password)
        error { codec.decrypt(file.copyOf(20), password) }
        ByteBuffer.wrap(file, 10, 4).putInt(-1)
        error(BackupError.AUTHENTICATION) { codec.decrypt(file, password) }
        ByteBuffer.wrap(file, 10, 4).putInt(0)
        error(BackupError.AUTHENTICATION) { codec.decrypt(file, password) }
    }
    @Test fun emptyDocumentRoundTrip() {
        val empty = document().copy(periods = emptyList(), dayLogs = emptyList())
        assertEquals(empty, BackupDocument.decode(codec.decrypt(codec.encrypt(empty.encode(), password), password)))
    }
    @Test fun twentyYearsRoundTrip() {
        val periods = (0 until 260).map { val start = today.minusDays((260L - it) * 28); Period(start = start, end = start.plusDays(4)) }
        val logs = (0 until 7300).map { DayLog(today.minusDays(it.toLong()), note = "Fictional example $it", mood = Mood.CALM) }
        val document = BackupDocument.from(periods, logs, AppSettings(), "2026-10-24T00:00:00Z", "0.1.0", 100)
        assertEquals(document, BackupDocument.decode(codec.decrypt(codec.encrypt(document.encode(), password), password)))
    }
    @Test fun securitySettingsNeverAppearInBackup() {
        val encoded = document().encode().toString(Charsets.UTF_8)
        listOf("lockEnabled", "lockBiometric", "lockTimeoutSeconds", "secureScreen", "pin_hash", "pin_salt", "db_key").forEach { assertFalse(encoded.contains(it)) }
        val applied = document().settings.applyTo(AppSettings(lockEnabled = true, secureScreen = true))
        assertTrue(applied.lockEnabled); assertTrue(applied.secureScreen)
    }
    @Test fun unknownFieldsAndEnumsAreIgnoredIndividually() {
        val original = document().copy(dayLogs = listOf(BackupDayLog(today.toString(), flow = "FUTURE", mood = "FUTURE", symptoms = listOf("FUTURE", "FATIGUE"), intercourse = "FUTURE", note = "Kept")))
        val encoded = original.encode().toString(Charsets.UTF_8).replace("\"format\":", "\"newField\":true,\"format\":")
        val log = BackupDocument.decode(encoded.toByteArray()).records(today).second.single()
        assertNull(log.flow); assertNull(log.mood); assertNull(log.intercourse)
        assertEquals(setOf(Symptom.FATIGUE), log.symptoms); assertEquals("Kept", log.note)
    }
    @Test fun invalidFutureAndReversedDatesAreRejected() {
        error(BackupError.INVALID_DATA) { document().copy(periods = listOf(BackupPeriod(today.plusDays(1).toString()))).records(today) }
        error(BackupError.INVALID_DATA) { document().copy(periods = listOf(BackupPeriod(today.toString(), today.minusDays(1).toString()))).records(today) }
        error(BackupError.INVALID_DATA) { document().copy(dayLogs = listOf(BackupDayLog(today.toString(), pain = 5))).records(today) }
    }
    @Test fun newestEqualStartEndWinsBeforeUnion() {
        val start = today.minusDays(30)
        val old = Period(start = start, end = start.plusDays(5), updatedAt = 1)
        val updated = old.copy(end = start.plusDays(3), updatedAt = 2)
        assertEquals(start.plusDays(3), mergeRecords(listOf(old), emptyList(), listOf(updated), emptyList(), today).periods.single().end)
    }
    @Test fun adjacentAndOverlappingPeriodsJoin() {
        val start = today.minusDays(30)
        val result = mergeRecords(listOf(Period(start = start, end = start.plusDays(2))), emptyList(),
            listOf(Period(start = start.plusDays(3), end = start.plusDays(5)), Period(start = start.plusDays(4), end = start.plusDays(8))), emptyList(), today)
        assertEquals(1, result.periods.size); assertEquals(start.plusDays(8), result.periods.single().end)
    }
    @Test fun latestWholeDayLogWinsAndResultCounts() {
        val result = mergeRecords(emptyList(), listOf(DayLog(today, note = "old", updatedAt = 1)), emptyList(),
            listOf(DayLog(today, pain = 2, updatedAt = 2), DayLog(today.minusDays(1), note = "added", updatedAt = 3)), today)
        assertNull(result.logs.last().note); assertEquals(2, result.logs.last().pain)
        assertEquals(1, result.result.addedDays); assertEquals(1, result.result.updatedDays)
    }
    @Test fun duplicateOpenPeriodsNormalizeToSingleLatestOpen() {
        val result = mergeRecords(listOf(Period(start = today.minusDays(10), end = null)), emptyList(), listOf(Period(start = today.minusDays(2), end = null)), emptyList(), today)
        assertEquals(1, result.periods.size); assertNull(result.periods.single().end)
    }
    @Test fun oversizedInputStreamIsBounded() { error(BackupError.TOO_LARGE) { BackupCodec.readLimited(ByteArray(20).inputStream(), 10) } }
    @Test fun productionIterationCountIsSixHundredThousand() {
        val file = BackupCodec().encrypt("{}".toByteArray(), password)
        assertEquals(600_000, ByteBuffer.wrap(file, 10, 4).int)
    }
    @Test fun csvHasBomRfcEscapesAndPrivateColumnIsOptional() {
        val labels = CsvLabels(listOf("日期", "经期", "天数", "经量", "痛经", "症状", "心情", "体温", "同房", "备注"), "是", "否", emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val csv = exportCsv(emptyList(), listOf(DayLog(today, note = "a,\"b\"\n第二行")), today, false, labels)
        assertArrayEquals(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()), csv.copyOfRange(0, 3))
        val text = csv.toString(Charsets.UTF_8)
        assertFalse(text.contains("同房")); assertTrue(text.contains("\"a,\"\"b\"\"\n第二行\""))
        assertTrue(exportCsv(emptyList(), emptyList(), today, true, labels).toString(Charsets.UTF_8).contains("同房"))
    }
    @Test fun csvIncludesPeriodDaysAndNeutralizesFormulaText() {
        val labels = CsvLabels((0..9).map(Int::toString), "Y", "N", emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val csv = exportCsv(listOf(Period(start = today.minusDays(1), end = today)), listOf(DayLog(today, note = "=1+1")), today, false, labels).toString(Charsets.UTF_8)
        assertTrue(csv.contains(today.minusDays(1).toString())); assertTrue(csv.contains("'=1+1")); assertEquals(4, csv.split("\r\n").size)
    }
}
