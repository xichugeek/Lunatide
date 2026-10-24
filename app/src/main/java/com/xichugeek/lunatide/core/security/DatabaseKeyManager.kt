package com.xichugeek.lunatide.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DataUnavailable(cause: Throwable? = null) : Exception("Local data cannot be unlocked", cause)

/** Wraps a random database password; never regenerates a key for existing inaccessible data. */
class DatabaseKeyManager(
    private val keyFile: File, private val databaseFile: File,
    private val alias: String = "lunatide_db_kek",
) {
    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized
    fun password(): ByteArray {
        try {
            val atomic = AtomicFile(keyFile)
            if (keyFile.exists() || File(keyFile.path + ".bak").exists()) {
                val blob = atomic.readFully()
                if (blob.size < 12 + 16 + 32) throw DataUnavailable()
                val key = store().getKey(alias, null) as? SecretKey ?: throw DataUnavailable()
                return Cipher.getInstance("AES/GCM/NoPadding").run {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob.copyOfRange(0, 12)))
                    doFinal(blob, 12, blob.size - 12)
                }.also { if (it.size != 32) { it.fill(0); throw DataUnavailable() } }
            }
            if (databaseFile.exists()) throw DataUnavailable()
            val key = (store().getKey(alias, null) as? SecretKey) ?: generate()
            val password = ByteArray(32).also(SecureRandom()::nextBytes)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
                val blob = cipher.iv + cipher.doFinal(password)
                keyFile.parentFile?.mkdirs()
                val output = atomic.startWrite()
                try { output.write(blob); atomic.finishWrite(output) }
                catch (error: Exception) { atomic.failWrite(output); throw error }
                return password
            } catch (error: Exception) { password.fill(0); throw error }
        } catch (error: Exception) {
            if (error is DataUnavailable) throw error
            throw DataUnavailable(error)
        }
    }

    private fun generate(): SecretKey {
        fun attempt(strongBox: Boolean): SecretKey {
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).apply { if (Build.VERSION.SDK_INT >= 28 && strongBox) setIsStrongBoxBacked(true) }.build()
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run { init(spec); generateKey() }
        }
        return if (Build.VERSION.SDK_INT >= 28) {
            try { attempt(true) } catch (_: StrongBoxUnavailableException) { attempt(false) }
        } else attempt(false)
    }

    fun delete() {
        AtomicFile(keyFile).delete()
        store().deleteEntry(alias)
    }
}
