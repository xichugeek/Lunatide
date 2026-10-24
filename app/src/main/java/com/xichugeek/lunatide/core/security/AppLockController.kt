package com.xichugeek.lunatide.core.security

import android.os.SystemClock
import androidx.lifecycle.*
import com.xichugeek.lunatide.data.AppDataReset
import com.xichugeek.lunatide.data.settings.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class PinMode { NONE, UNLOCK, SET, CONFIRM, CHANGE, DISABLE }
enum class LockError { WRONG_PIN, MISMATCH, STORAGE, BIOMETRIC }
data class LockUiState(
    val ready: Boolean = false, val locked: Boolean = true, val pinSet: Boolean = false,
    val settings: AppSettings = AppSettings(), val mode: PinMode = PinMode.NONE,
    val digits: String = "", val busy: Boolean = false, val cooldownSeconds: Int = 0,
    val error: LockError? = null, val erased: Boolean = false,
)

/** The process-wide access gate stays closed until both security stores have been read. */
@Singleton
class AppLockController @Inject constructor(
    private val settings: SettingsStore, private val security: SecurityRepository, private val reset: AppDataReset,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(LockUiState())
    val state = _state.asStateFlow()
    private var record = SecurityRecord()
    private var unlocked = false
    private var firstPin: CharArray? = null
    private var backgroundAt: Long? = null
    private var external = false
    private var exemptStop = false
    private var initialized = false
    fun start() {
        if (initialized) return
        initialized = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            combine(settings.settings, security.records) { s, r -> s to r }
                .catch { error -> if (error is CancellationException) throw error; _state.update { it.copy(error = LockError.STORAGE) } }
                .collect { (s, r) ->
                    record = r
                    val enabled = s.lockEnabled || r.digest != null
                    val locked = enabled && !unlocked
                    _state.update { it.copy(ready = true, settings = s, pinSet = r.digest != null, locked = locked,
                        cooldownSeconds = ((r.attempts.remaining(elapsed(), boot()) + 999) / 1_000).toInt(),
                        mode = if (locked) PinMode.UNLOCK else if (it.mode == PinMode.UNLOCK) PinMode.NONE else it.mode) }
                }
        }
        scope.launch {
            var ticks = 0
            while (isActive) {
                delay(1_000)
                val left = record.attempts.remaining(elapsed(), boot())
                _state.update { it.copy(cooldownSeconds = ((left + 999) / 1_000).toInt()) }
                if (left > 0 && ++ticks % 5 == 0) operation {
                    record = record.copy(attempts = record.attempts.at(elapsed(), boot()))
                    security.attempts(record.attempts)
                }
            }
        }
    }
    override fun onStop(owner: LifecycleOwner) = background()
    override fun onStart(owner: LifecycleOwner) = foreground()
    fun externalJump() { external = true }
    fun externalReturned() { external = false; exemptStop = false; backgroundAt = null }
    fun background() {
        if (backgroundAt != null) return
        backgroundAt = elapsed(); exemptStop = external
        if (!exemptStop && _state.value.settings.lockTimeoutSeconds == 0) lock()
    }
    fun foreground() {
        val stopped = backgroundAt
        if (stopped != null && !exemptStop && elapsed() - stopped >= _state.value.settings.lockTimeoutSeconds * 1_000L) lock()
        backgroundAt = null; exemptStop = false; external = false
    }
    private fun lock() {
        if (!_state.value.pinSet && !_state.value.settings.lockEnabled) return
        unlocked = false; clearFirst()
        _state.update { it.copy(locked = true, mode = PinMode.UNLOCK, digits = "", error = null) }
    }
    fun begin(mode: PinMode) {
        if (_state.value.busy || _state.value.locked || mode !in listOf(PinMode.SET, PinMode.CHANGE, PinMode.DISABLE)) return
        if (mode == PinMode.SET && _state.value.pinSet) return
        clearFirst(); _state.update { it.copy(mode = mode, digits = "", error = null) }
    }
    fun dismiss() { if (!_state.value.locked && !_state.value.busy) { clearFirst(); _state.update { it.copy(mode = PinMode.NONE, digits = "", error = null) } } }
    fun digit(value: String) {
        if (_state.value.busy || _state.value.cooldownSeconds > 0) return
        _state.update { it.copy(digits = if (value == "BACK") it.digits.dropLast(1) else (it.digits + value.filter(Char::isDigit)).take(6)) }
    }
    fun submit() {
        val current = _state.value
        if (current.busy || record.attempts.remaining(elapsed(), boot()) > 0 || current.digits.length !in 4..6 || current.mode == PinMode.NONE) return
        val pin = current.digits.toCharArray()
        _state.update { it.copy(busy = true, digits = "", error = null) }
        operation {
            try {
                when (current.mode) {
                    PinMode.SET -> { clearFirst(); firstPin = pin.copyOf(); _state.update { it.copy(mode = PinMode.CONFIRM) } }
                    PinMode.CONFIRM -> {
                        val first = firstPin
                        if (first == null || !PinCrypto.sameInput(first, pin)) {
                            clearFirst(); _state.update { it.copy(mode = PinMode.SET, error = LockError.MISMATCH) }
                        } else {
                            val digest = withContext(Dispatchers.Default) { PinCrypto.create(pin) }
                            unlocked = true
                            security.setPin(digest)
                            settings.update { it.copy(lockEnabled = true, secureScreen = true) }
                            clearFirst(); _state.update { it.copy(mode = PinMode.NONE, locked = false, pinSet = true) }
                        }
                    }
                    else -> {
                        val digest = record.digest
                        val valid = digest != null && withContext(Dispatchers.Default) { PinCrypto.verify(pin, digest) }
                        if (valid) {
                            security.attempts(PinAttempts()); record = record.copy(attempts = PinAttempts())
                            when (current.mode) {
                                PinMode.CHANGE -> _state.update { it.copy(mode = PinMode.SET) }
                                PinMode.DISABLE -> {
                                    settings.update { it.copy(lockEnabled = false, lockBiometric = false) }
                                    security.clear(); unlocked = true; _state.update { it.copy(mode = PinMode.NONE, locked = false) }
                                }
                                else -> { unlocked = true; _state.update { it.copy(mode = PinMode.NONE, locked = false) } }
                            }
                        } else {
                            record = record.copy(attempts = record.attempts.failed(elapsed(), boot()))
                            security.attempts(record.attempts)
                            _state.update { it.copy(error = LockError.WRONG_PIN, cooldownSeconds = ((record.attempts.remainingMs + 999) / 1_000).toInt()) }
                        }
                    }
                }
            } finally { pin.fill('0'); _state.update { it.copy(busy = false) } }
        }
    }
    fun biometricSuccess() {
        if (!_state.value.settings.lockBiometric || !_state.value.pinSet || _state.value.cooldownSeconds > 0) return
        operation { security.attempts(PinAttempts()); unlocked = true; _state.update { it.copy(locked = false, mode = PinMode.NONE, error = null) } }
    }
    fun biometricError() { _state.update { it.copy(error = LockError.BIOMETRIC) } }
    fun update(transform: (AppSettings) -> AppSettings) { operation { settings.update(transform) } }
    fun erase() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        operation { reset.erase(); clearFirst(); unlocked = false; _state.value = LockUiState(erased = true) }
    }
    fun acknowledgeErase() { _state.value = LockUiState(ready = true, locked = false) }
    private fun operation(block: suspend () -> Unit) { scope.launch {
        mutex.withLock {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { _state.update { it.copy(error = LockError.STORAGE, busy = false) } }
        }
    } }
    private fun clearFirst() { firstPin?.fill('0'); firstPin = null }
    private fun elapsed() = SystemClock.elapsedRealtime()
    private fun boot() = System.currentTimeMillis() - elapsed()
}
