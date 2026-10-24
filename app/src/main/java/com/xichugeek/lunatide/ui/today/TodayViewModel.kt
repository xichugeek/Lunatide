package com.xichugeek.lunatide.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.*
import com.xichugeek.lunatide.data.settings.*
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.prediction.*
import com.xichugeek.lunatide.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class TodayUiState(
    val ready: Boolean = false, val failed: Boolean = false, val periods: List<Period> = emptyList(),
    val logs: List<DayLog> = emptyList(), val settings: AppSettings = AppSettings(),
    val today: LocalDate = LocalDate.of(1970, 1, 1), val prediction: PredictionResult? = null,
)
data class TodayNotice(val action: PeriodAction, val before: List<Period>, val day: LocalDate)
data class ActionState(val busy: Boolean = false, val issue: EditIssue? = null, val storageError: Boolean = false, val notice: TodayNotice? = null)
enum class HomeStage { WELCOME, ONGOING, WAITING, DUE, LATE }
data class HomeStatus(val stage: HomeStage, val days: Int = 0, val remaining: Int = 0)
fun TodayUiState.homeStatus(): HomeStatus {
    val ongoing = periods.lastOrNull { it.end == null && it.start <= today }
    val prediction = prediction
    return when {
        ongoing != null -> {
            val day = (today.toEpochDay() - ongoing.start.toEpochDay() + 1).toInt()
            HomeStatus(HomeStage.ONGOING, day, (prediction?.periodLength ?: 5) - day)
        }
        prediction?.nextPeriodStart == null -> HomeStatus(HomeStage.WELCOME)
        prediction.daysLate > 0 -> HomeStatus(HomeStage.LATE, prediction.daysLate)
        today == prediction.nextPeriodStart -> HomeStatus(HomeStage.DUE)
        else -> HomeStatus(HomeStage.WAITING, (prediction.nextPeriodStart.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val cycles: CycleRepository, private val logs: DayLogRepository,
    private val settings: SettingsStore, private val clock: TodayProvider,
) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val state: StateFlow<TodayUiState> = retry.flatMapLatest {
        combine(cycles.periods, logs.logs, settings.settings, clock.today) { periods, logs, settings, today ->
            withContext(Dispatchers.Default) {
                TodayUiState(true, false, periods, logs, settings, today, predict(periods, today, settings.toPredictionConfig()))
            }
        }.catch { error -> if (error is CancellationException) throw error; emit(TodayUiState(failed = true, today = clock.today.value)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TodayUiState(today = clock.today.value))
    private val _actions = MutableStateFlow(ActionState())
    val actions = _actions.asStateFlow()
    private data class Request(val action: PeriodAction, val day: LocalDate, val before: List<Period>, val revise: Boolean)
    private var pending: Request? = null

    fun refresh() { clock.refresh() }
    fun retry() { retry.value++ }
    fun clearMessage() { _actions.value = _actions.value.copy(issue = null, storageError = false, notice = null) }
    fun record(day: LocalDate = clock.today.value, recent: Boolean = false, revise: Boolean = false) {
        if (_actions.value.busy || !state.value.ready) return
        val notice = _actions.value.notice
        val previousRequest = pending.takeIf { recent }
        val before = previousRequest?.before ?: if (revise && notice != null) notice.before else state.value.periods
        val action = previousRequest?.action ?: if (revise && notice != null) notice.action else if (before.any { it.end == null }) PeriodAction.END else PeriodAction.START
        val requestedDay = previousRequest?.day ?: day
        val useBase = previousRequest?.revise ?: revise
        pending = Request(action, requestedDay, before, useBase)
        _actions.value = ActionState(busy = true)
        viewModelScope.launch {
            val result = cycles.edit(action, requestedDay, recent = recent, base = if (useBase) before else null)
            _actions.value = result.fold(
                onSuccess = { when (it) {
                    is PeriodEdit.Applied -> ActionState(notice = TodayNotice(action, before, requestedDay))
                    is PeriodEdit.Rejected -> ActionState(issue = it.issue)
                } },
                onFailure = { ActionState(storageError = true) },
            )
        }
    }
    fun undo() {
        val notice = _actions.value.notice ?: return
        if (_actions.value.busy) return
        _actions.value = ActionState(busy = true)
        viewModelScope.launch { _actions.value = if (cycles.restorePeriods(notice.before).isSuccess) ActionState() else ActionState(storageError = true) }
    }
    fun spotting() {
        _actions.value = ActionState(busy = true)
        viewModelScope.launch {
            val current = state.value.logs.firstOrNull { it.day == clock.today.value } ?: DayLog(clock.today.value)
            val result = logs.save(current.copy(flow = com.xichugeek.lunatide.domain.model.Flow.SPOTTING))
            _actions.value = ActionState(storageError = result.isFailure)
        }
    }
}
