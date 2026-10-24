package com.xichugeek.lunatide.domain

import com.xichugeek.lunatide.core.security.*
import org.junit.Assert.*
import org.junit.Test

class PinSecurityTest {
    @Test fun pinLengthAndAsciiOnly() {
        listOf("1234", "12345", "123456").forEach { assertTrue(PinCrypto.valid(it.toCharArray())) }
        listOf("", "123", "1234567", "１２３４", "12a4", " 1234").forEach { assertFalse(PinCrypto.valid(it.toCharArray())) }
    }
    @Test fun hashRoundTripRandomSaltAndWrongPin() {
        val pin = "123456".toCharArray()
        val first = PinCrypto.create(pin); val second = PinCrypto.create(pin)
        assertEquals(16, first.salt.size); assertEquals(32, first.hash.size); assertTrue(first.iterations >= 120_000)
        assertFalse(first.salt.contentEquals(second.salt)); assertFalse(first.hash.contentEquals(second.hash))
        assertTrue(PinCrypto.verify(pin, first)); assertFalse(PinCrypto.verify("123455".toCharArray(), first))
    }
    @Test fun invalidDigestIsRejected() {
        assertFalse(PinCrypto.verify("1234".toCharArray(), PinDigest(ByteArray(0), ByteArray(32))))
        assertFalse(PinCrypto.verify("1234".toCharArray(), PinDigest(ByteArray(16), ByteArray(0))))
        assertFalse(PinCrypto.verify("1234".toCharArray(), PinDigest(ByteArray(16), ByteArray(32), 1)))
    }
    @Test fun fiveWrongPinsStartsThirtySeconds() {
        var attempts = PinAttempts()
        repeat(4) { attempts = attempts.failed(1_000, 1_000_000); assertEquals(0L, attempts.remainingMs) }
        attempts = attempts.failed(1_000, 1_000_000)
        assertEquals(30_000L, attempts.remainingMs)
        assertEquals(25_000L, attempts.remaining(6_000, 1_000_000))
        assertEquals(0L, attempts.remaining(31_000, 1_000_000))
    }
    @Test fun laterGroupsDoubleAndCapAtThirtyMinutes() {
        var attempts = PinAttempts()
        repeat(10) { attempts = attempts.failed(100_000, 2_000_000) }
        assertEquals(60_000L, attempts.remainingMs)
        repeat(990) { attempts = attempts.failed(100_000, 2_000_000) }
        assertEquals(1_800_000L, attempts.remainingMs)
    }
    @Test fun processRestartPreservesRemaining() {
        val saved = PinAttempts(5, 30_000, 100_000, 1_000_000).at(110_000, 1_000_000)
        assertEquals(20_000L, saved.remainingMs)
        assertEquals(18_000L, saved.remaining(112_000, 1_000_000))
    }
    @Test fun rebootOrClockChangeConservativelyRestartsSavedRemainder() {
        val saved = PinAttempts(5, 20_000, 100_000, 1_000_000)
        assertEquals(20_000L, saved.remaining(100, 2_000_000))
        val rebased = saved.at(100, 2_000_000)
        assertEquals(19_000L, rebased.remaining(1_100, 2_000_000))
        assertEquals(20_000L, saved.remaining(150_000, 3_000_000))
    }
}
