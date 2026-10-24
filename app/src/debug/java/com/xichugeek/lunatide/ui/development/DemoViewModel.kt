package com.xichugeek.lunatide.ui.development

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.BuildConfig
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.backup.BackupDocument
import com.xichugeek.lunatide.data.backup.BackupRepository
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.model.Flow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DemoState(val busy: Boolean = false, val done: Boolean = false, val failed: Boolean = false)
@HiltViewModel
class DemoViewModel @Inject constructor(private val backups: BackupRepository, private val clock: TodayProvider) : ViewModel() {
    private val _state = MutableStateFlow(DemoState())
    val state = _state.asStateFlow()
    fun fill(performance: Boolean = false) {
        if (_state.value.busy) return
        _state.value = DemoState(busy = true)
        viewModelScope.launch {
            try {
                clock.refresh()
                val today = clock.today.value
                var start = today.minusDays(9)
                val starts = mutableListOf(start)
                val lengths = if (performance) List(259) { 28 } else listOf(29, 28, 30, 27, 31, 29, 28)
                lengths.forEach { length -> start = start.minusDays(length.toLong()); starts += start }
                val periods = starts.sorted().mapIndexed { index, day -> Period(start = day, end = day.plusDays(if (index % 2 == 0) 4 else 5), updatedAt = clock.timestamp()) }
                val logs = if (performance) withContext(Dispatchers.Default) { (0 until 7300).map { offset ->
                    val day = today.minusDays(offset.toLong())
                    DayLog(day, flow = if (periods.any { it.contains(day, today) }) Flow.LIGHT else null,
                        symptoms = if (offset % 17 == 0) setOf(Symptom.FATIGUE) else emptySet(), mood = Mood.CALM,
                        note = if (offset % 7 == 0) "早点睡" else null, updatedAt = clock.timestamp())
                } } else periods.flatMapIndexed { index, period ->
                    listOf(DayLog(period.start, flow = Flow.MEDIUM, pain = index % 3, symptoms = setOf(Symptom.FATIGUE), mood = Mood.CALM,
                        note = "多喝热水", updatedAt = clock.timestamp()),
                        DayLog(period.start.plusDays(2), flow = Flow.LIGHT, pain = 0, mood = Mood.HAPPY, note = "早点睡", updatedAt = clock.timestamp()))
                } + DayLog(today, symptoms = setOf(Symptom.FATIGUE), mood = Mood.CALM, note = "今天早一点休息", updatedAt = clock.timestamp())
                val document = BackupDocument.from(periods, logs, AppSettings(onboardingCompleted = true, theme = "LIGHT"),
                    Instant.ofEpochMilli(clock.timestamp()).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString(), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                backups.restore(document, merge = false)
                _state.value = DemoState(done = true)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _state.value = DemoState(failed = true) }
        }
    }
}
