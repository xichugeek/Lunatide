package com.xichugeek.lunatide.data.settings

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.xichugeek.lunatide.core.security.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

private val Context.securityStore by preferencesDataStore("security")
data class SecurityRecord(val digest: PinDigest? = null, val attempts: PinAttempts = PinAttempts())

@Singleton
class SecurityRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.securityStore
    val records = store.data.map { p ->
        val hash = p[stringPreferencesKey("pin_hash")]
        val salt = p[stringPreferencesKey("pin_salt")]
        val digest = if (hash == null && salt == null) null else PinDigest(
            Base64.decode(requireNotNull(salt), Base64.NO_WRAP), Base64.decode(requireNotNull(hash), Base64.NO_WRAP),
            p[intPreferencesKey("pin_iterations")] ?: 120_000)
        SecurityRecord(digest, PinAttempts(p[intPreferencesKey("failed_attempts")] ?: 0,
            p[longPreferencesKey("remaining_ms")] ?: 0, p[longPreferencesKey("anchor_elapsed")] ?: 0, p[longPreferencesKey("boot_estimate")] ?: 0))
    }
    suspend fun setPin(digest: PinDigest) { store.edit { p ->
        p[stringPreferencesKey("pin_hash")] = Base64.encodeToString(digest.hash, Base64.NO_WRAP)
        p[stringPreferencesKey("pin_salt")] = Base64.encodeToString(digest.salt, Base64.NO_WRAP)
        p[intPreferencesKey("pin_iterations")] = digest.iterations
    }; attempts(PinAttempts()) }
    suspend fun attempts(value: PinAttempts) { store.edit { p ->
        p[intPreferencesKey("failed_attempts")] = value.failures
        p[longPreferencesKey("remaining_ms")] = value.remainingMs
        p[longPreferencesKey("anchor_elapsed")] = value.anchorElapsed
        p[longPreferencesKey("boot_estimate")] = value.bootEstimate
    } }
    suspend fun clear() { store.edit { it.clear() } }
}
