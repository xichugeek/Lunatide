package com.xichugeek.lunatide.security

import androidx.test.platform.app.InstrumentationRegistry
import com.xichugeek.lunatide.core.security.*
import com.xichugeek.lunatide.data.settings.SecurityRepository
import com.xichugeek.lunatide.data.settings.SettingsRepository
import com.xichugeek.lunatide.LunatideApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SecurityStoreTest {
    @Test fun controllerLocksOnBackgroundAndBlocksAfterFiveFailures() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val security = SecurityRepository(context)
        val settings = SettingsRepository(context)
        val controller = (context.applicationContext as LunatideApp).appLock
        suspend fun await(predicate: (LockUiState) -> Boolean) = withTimeout(10_000) { controller.state.first(predicate) }
        security.clear(); settings.clear()
        try {
            await { it.ready && !it.locked && !it.pinSet }
            withContext(Dispatchers.Main) {
                controller.begin(PinMode.SET); "739251".forEach { controller.digit(it.toString()) }; controller.submit()
            }
            await { it.mode == PinMode.CONFIRM && !it.busy }
            withContext(Dispatchers.Main) { "739251".forEach { controller.digit(it.toString()) }; controller.submit() }
            await { it.pinSet && !it.locked && !it.busy && it.settings.secureScreen }
            withContext(Dispatchers.Main) { controller.background(); controller.foreground() }
            await { it.locked && it.mode == PinMode.UNLOCK }
            repeat(5) {
                withContext(Dispatchers.Main) { repeat(4) { controller.digit("0") }; controller.submit() }
                await { !it.busy && it.error == LockError.WRONG_PIN }
            }
            val locked = await { it.cooldownSeconds > 0 }
            assertTrue(locked.cooldownSeconds in 1..30)
            assertEquals(5, SecurityRepository(context).records.first().attempts.failures)
            withContext(Dispatchers.Main) { controller.digit("1"); controller.submit() }
            assertEquals("", controller.state.value.digits)
            assertTrue(controller.state.value.locked)
        } finally { settings.clear(); security.clear() }
    }
    @Test fun pinAndCooldownSurviveRepositoryRecreationWithoutPlainPin() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SecurityRepository(context)
        repository.clear()
        try {
            val pin = "739251".toCharArray()
            repository.setPin(PinCrypto.create(pin))
            val attempts = PinAttempts(5, 30_000, 100_000, 1_000_000)
            repository.attempts(attempts)
            val reopened = SecurityRepository(context).records.first()
            assertTrue(PinCrypto.verify(pin, requireNotNull(reopened.digest)))
            assertEquals(attempts, reopened.attempts)
            val file = java.io.File(context.filesDir, "datastore/security.preferences_pb")
            assertTrue(file.exists())
            assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains(String(pin)))
            pin.fill('0')
        } finally { repository.clear() }
    }
}
