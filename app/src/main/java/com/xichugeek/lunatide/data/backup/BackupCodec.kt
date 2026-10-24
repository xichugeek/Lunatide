package com.xichugeek.lunatide.data.backup

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class BackupError { NOT_BACKUP, NEWER_VERSION, AUTHENTICATION, INVALID_DATA, TOO_LARGE, STORAGE }
class BackupException(val reason: BackupError) : Exception(reason.name)

/** Version 1: 42 authenticated header bytes, PBKDF2-SHA256, AES-256-GCM, GZIP UTF-8 JSON. */
class BackupCodec internal constructor(private val exportIterations: Int = 600_000) {
    fun encrypt(json: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 6)
        if (json.size > MAX_PLAIN_BYTES) throw BackupException(BackupError.TOO_LARGE)
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val header = ByteBuffer.allocate(42).put(MAGIC).put(1).put(1).putInt(exportIterations).put(salt).put(iv).array()
        val compressed = ByteArrayOutputStream().use { out -> GZIPOutputStream(out).use { it.write(json) }; out.toByteArray() }
        val key = derive(password, salt, exportIterations)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            cipher.updateAAD(header)
            header + cipher.doFinal(compressed)
        } finally { key.fill(0); compressed.fill(0) }
    }
    fun decrypt(file: ByteArray, password: CharArray): ByteArray {
        if (file.size < 8 || !file.copyOfRange(0, 8).contentEquals(MAGIC)) throw BackupException(BackupError.NOT_BACKUP)
        if (file.size < 58) throw BackupException(BackupError.AUTHENTICATION)
        if ((file[8].toInt() and 255) > 1) throw BackupException(BackupError.NEWER_VERSION)
        if (file[8] != 1.toByte() || file[9] != 1.toByte()) throw BackupException(BackupError.AUTHENTICATION)
        if (file.size > MAX_FILE_BYTES) throw BackupException(BackupError.TOO_LARGE)
        val iterations = ByteBuffer.wrap(file, 10, 4).int.toLong() and 0xffffffffL
        if (iterations !in 1..1_000_000) throw BackupException(BackupError.AUTHENTICATION)
        val key = derive(password, file.copyOfRange(14, 30), iterations.toInt())
        val compressed = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, file.copyOfRange(30, 42)))
            cipher.updateAAD(file, 0, 42)
            cipher.doFinal(file, 42, file.size - 42)
        } catch (_: Exception) { throw BackupException(BackupError.AUTHENTICATION) }
        finally { key.fill(0) }
        return try { GZIPInputStream(compressed.inputStream()).use { readLimited(it, MAX_PLAIN_BYTES) } }
        catch (error: BackupException) { throw error }
        catch (_: Exception) { throw BackupException(BackupError.INVALID_DATA) }
        finally { compressed.fill(0) }
    }
    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    companion object {
        val MAGIC: ByteArray get() = "LUNATIDE".toByteArray(Charsets.US_ASCII)
        const val MAX_FILE_BYTES = 16 * 1024 * 1024
        const val MAX_PLAIN_BYTES = 32 * 1024 * 1024
        fun readLimited(input: java.io.InputStream, limit: Int = MAX_FILE_BYTES): ByteArray {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (out.size().toLong() + count > limit) throw BackupException(BackupError.TOO_LARGE)
                out.write(buffer, 0, count)
            }
            return out.toByteArray()
        }
    }
}
