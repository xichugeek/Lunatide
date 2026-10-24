package com.xichugeek.lunatide.ui.daylog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.*
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.xichugeek.lunatide.domain.model.Flow as MenstrualFlow

enum class EditorPrompt { INCLUDE_FOR_FLOW, RECENT, SHORTEN, CLEAR }
data class DayEditorState(
    val day: LocalDate, val log: DayLog = DayLog(day), val inPeriod: Boolean = false,
    val future: Boolean = false, val loading: Boolean = true, val busy: Boolean = false,
    val saving: Boolean = false, val failed: Boolean = false, val dirty: Boolean = false,
    val temperatureText: String = "", val temperatureInvalid: Boolean = false,
    val prompt: EditorPrompt? = null, val pendingFlow: MenstrualFlow? = null,
    val pendingPeriod: Boolean? = null, val version: Int = 0, val recentGap: Int = 0,
)
data class DayEditorActions(
    val close: () -> Unit = {}, val flow: (MenstrualFlow?) -> Unit = {}, val pain: (Int?) -> Unit = {},
    val mood: (Mood?) -> Unit = {}, val symptom: (Symptom) -> Unit = {}, val temperature: (String) -> Unit = {},
    val intercourse: (Intercourse?) -> Unit = {}, val note: (String) -> Unit = {}, val period: (Boolean) -> Unit = {},
    val confirm: () -> Unit = {}, val cancel: () -> Unit = {}, val spotting: () -> Unit = {}, val clear: () -> Unit = {},
)

/** Holds drafts in memory; no saved-state bundle or plain-text preferences contain notes. */
@HiltViewModel
class DayLogViewModel @Inject constructor(
    private val logs: DayLogRepository, private val cycles: CycleRepository, private val clock: TodayProvider,
) : ViewModel() {
    private val _state = MutableStateFlow<DayEditorState?>(null)
    val state = _state.asStateFlow()
    private var pendingSave: Job? = null
    private var loadingJob: Job? = null
    private val mutex = Mutex()
    val actions = DayEditorActions(::close, ::setFlow, ::setPain, ::setMood, ::toggleSymptom, ::setTemperature,
        ::setIntercourse, ::setNote, ::setPeriod, ::confirm, ::cancel, ::spotting, ::clear)

    fun open(day: LocalDate) {
        if (_state.value != null) return
        _state.value = DayEditorState(day, future = day > clock.today.value)
        loadingJob = viewModelScope.launch {
            try {
                val log = logs.logs.first().firstOrNull { it.day == day } ?: DayLog(day)
                val periods = cycles.periods.first()
                val inPeriod = periods.any { it.contains(day, clock.today.value) }
                val previousEnd = periods.mapNotNull { it.end }.filter { it < day }.maxOrNull()
                _state.value = DayEditorState(day, log, inPeriod, future = day > clock.today.value, loading = false,
                    temperatureText = log.bbtCentiCelsius?.let { BigDecimal(it).movePointLeft(2).toPlainString() }.orEmpty(),
                    recentGap = previousEnd?.let { (day.toEpochDay() - it.toEpochDay()).toInt() } ?: 0)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _state.value = _state.value?.copy(loading = false, failed = true) }
        }
    }
    private fun change(transform: (DayLog) -> DayLog) {
        val state = _state.value ?: return
        if (state.future || state.loading || state.busy) return
        val log = transform(state.log)
        if (log == state.log) return
        _state.value = state.copy(log = log, dirty = true, failed = false, version = state.version + 1)
        scheduleSave()
    }
    private fun scheduleSave() {
        pendingSave?.cancel()
        pendingSave = viewModelScope.launch { delay(300); persist() }
    }
    private suspend fun persist(): Boolean = mutex.withLock {
        val state = _state.value ?: return@withLock true
        if (state.future) return@withLock true
        if (state.temperatureInvalid) return@withLock false
        if (!state.dirty) return@withLock true
        _state.value = state.copy(saving = true)
        val result = logs.save(state.log)
        _state.update { current -> current?.copy(saving = false, failed = result.isFailure,
            dirty = if (result.isSuccess && current.version == state.version) false else current.dirty) }
        result.isSuccess
    }
    private suspend fun cancelPending() { val job = pendingSave; pendingSave = null; job?.cancelAndJoin() }
    fun close() {
        if (_state.value?.busy == true) return
        viewModelScope.launch { loadingJob?.cancelAndJoin(); cancelPending(); if (persist()) _state.value = null }
    }
    fun setNote(value: String) = change { it.copy(note = value.take(500).takeIf(String::isNotBlank)) }
    fun setPain(value: Int?) = change { it.copy(pain = value) }
    fun setMood(value: Mood?) = change { it.copy(mood = value) }
    fun setIntercourse(value: Intercourse?) = change { it.copy(intercourse = value) }
    fun toggleSymptom(value: Symptom) = change { it.copy(symptoms = if (value in it.symptoms) it.symptoms - value else it.symptoms + value) }
    fun setTemperature(value: String) {
        val state = _state.value ?: return
        if (state.future || state.busy || state.loading) return
        val number = if (Regex("\\d{1,2}(\\.\\d{0,2})?").matches(value)) value.toBigDecimalOrNull() else null
        val valid = value.isBlank() || (number != null && number >= BigDecimal("34.0") && number <= BigDecimal("42.0"))
        _state.value = state.copy(temperatureText = value, temperatureInvalid = !valid)
        if (valid) change { it.copy(bbtCentiCelsius = number?.movePointRight(2)?.setScale(0, RoundingMode.UNNECESSARY)?.toInt()) }
    }
    fun setFlow(value: MenstrualFlow?) {
        val state = _state.value ?: return
        if (state.future || state.busy || state.loading) return
        if (value != null && value != MenstrualFlow.SPOTTING && !state.inPeriod) {
            _state.value = state.copy(prompt = EditorPrompt.INCLUDE_FOR_FLOW, pendingFlow = value, pendingPeriod = true)
        } else change { it.copy(flow = value) }
    }
    fun setPeriod(value: Boolean) { applyPeriod(value, recent = false, shorten = false) }
    private fun applyPeriod(value: Boolean, recent: Boolean, shorten: Boolean) {
        val state = _state.value ?: return
        if (state.future || state.loading || state.busy || state.temperatureInvalid) return
        _state.value = state.copy(busy = true, failed = false)
        viewModelScope.launch {
            try {
            cancelPending()
            val current = _state.value ?: return@launch
            val log = current.pendingFlow?.let { current.log.copy(flow = it) } ?: current.log
            val result = mutex.withLock { logs.editDay(log, value, recent, shorten) }
            val edit = result.getOrNull()
            if (edit is PeriodEdit.Rejected) {
                _state.value = current.copy(busy = false, pendingPeriod = value, prompt = when (edit.issue) {
                    EditIssue.CONFIRM_RECENT -> EditorPrompt.RECENT
                    EditIssue.CONFIRM_SHORTEN -> EditorPrompt.SHORTEN
                    else -> null
                }, failed = edit.issue !in listOf(EditIssue.CONFIRM_RECENT, EditIssue.CONFIRM_SHORTEN), saving = false)
            } else if (result.isFailure) _state.value = current.copy(busy = false, failed = true, saving = false)
            else {
                val saved = logs.logs.first().firstOrNull { it.day == state.day } ?: DayLog(state.day)
                _state.value = current.copy(log = saved, inPeriod = value, dirty = false, busy = false, saving = false, prompt = null, pendingFlow = null, pendingPeriod = null)
            }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _state.update { it?.copy(busy = false, saving = false, failed = true) } }
        }
    }
    fun confirm() {
        val state = _state.value ?: return
        when (state.prompt) {
            EditorPrompt.INCLUDE_FOR_FLOW -> applyPeriod(true, recent = false, shorten = false)
            EditorPrompt.RECENT -> applyPeriod(state.pendingPeriod ?: true, recent = true, shorten = false)
            EditorPrompt.SHORTEN -> applyPeriod(false, recent = false, shorten = true)
            EditorPrompt.CLEAR -> { cancel(); change { DayLog(it.day) }; _state.update { it?.copy(temperatureText = "", temperatureInvalid = false) } }
            null -> Unit
        }
    }
    fun cancel() { _state.update { it?.copy(prompt = null, pendingFlow = null, pendingPeriod = null) } }
    fun spotting() { cancel(); setFlow(MenstrualFlow.SPOTTING) }
    fun clear() { _state.update { if (it?.future == false) it.copy(prompt = EditorPrompt.CLEAR) else it } }
}
