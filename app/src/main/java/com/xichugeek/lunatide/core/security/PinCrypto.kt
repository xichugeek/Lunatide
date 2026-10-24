package com.xichugeek.lunatide.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinDigest(val salt: ByteArray, val hash: ByteArray, val iterations: Int = 120_000)

/** PIN controls UI access; it never derives the database key. Run KDF work off the main thread. */
object PinCrypto {
    fun valid(pin: CharArray) = pin.size in 4..6 && pin.all { it in '0'..'9' }
    fun sameInput(first: CharArray, second: CharArray): Boolean {
        if (first.size != second.size) return false
        var difference = 0
        first.indices.forEach { difference = difference or (first[it].code xor second[it].code) }
        return difference == 0
    }
    fun create(pin: CharArray): PinDigest {
        require(valid(pin))
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        return PinDigest(salt, derive(pin, salt, 120_000))
    }
    fun verify(pin: CharArray, digest: PinDigest): Boolean {
        if (!valid(pin) || digest.salt.size != 16 || digest.hash.size != 32 || digest.iterations !in 120_000..1_000_000) return false
        val candidate = derive(pin, digest.salt, digest.iterations)
        return try { MessageDigest.isEqual(candidate, digest.hash) } finally { candidate.fill(0) }
    }
    private fun derive(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}

data class PinAttempts(val failures: Int = 0, val remainingMs: Long = 0, val anchorElapsed: Long = 0, val bootEstimate: Long = 0) {
    /** A changed boot origin (or backwards uptime) conservatively restarts the last saved remainder. */
    fun remaining(now: Long, boot: Long): Long = if (now < anchorElapsed || kotlin.math.abs(boot - bootEstimate) > 120_000) remainingMs
        else (remainingMs - (now - anchorElapsed)).coerceAtLeast(0)
    fun at(now: Long, boot: Long) = copy(remainingMs = remaining(now, boot), anchorElapsed = now, bootEstimate = boot)
    fun failed(now: Long, boot: Long): PinAttempts {
        val count = (failures + 1).coerceAtMost(1_000_000)
        val delay = if (count % 5 == 0) (30_000L * (1L shl ((count / 5 - 1).coerceAtMost(6)))).coerceAtMost(1_800_000) else 0
        return PinAttempts(count, delay, now, boot)
    }
}
