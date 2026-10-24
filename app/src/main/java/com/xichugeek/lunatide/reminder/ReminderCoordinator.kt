package com.xichugeek.lunatide.reminder

import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.*
import com.xichugeek.lunatide.data.settings.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
@Singleton
class ReminderCoordinator @Inject constructor(
    private val cycles: CycleRepository, private val logs: DayLogRepository,
    private val settings: SettingsStore, private val clock: TodayProvider, private val scheduler: ReminderScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observing: Job? = null
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()
    fun start() {
        observing?.cancel()
        observing = scope.launch {
            combine(cycles.periods, logs.logs, settings.settings, clock.today) { periods, _, settings, _ -> periods to settings }
                .debounce(500).catch { error -> if (error is CancellationException) throw error; _failed.value = true; runCatching { scheduler.cancelScheduled() } }
                .collect { (periods, settings) ->
                    try { scheduler.reschedule(periods, settings); _failed.value = false }
                    catch (error: CancellationException) { throw error }
                    catch (_: Exception) { _failed.value = true; runCatching { scheduler.cancelScheduled() } }
                }
        }
    }
    suspend fun stop() { observing?.cancelAndJoin(); observing = null; scheduler.cancelScheduled() }
    fun refresh() { start() }
}
