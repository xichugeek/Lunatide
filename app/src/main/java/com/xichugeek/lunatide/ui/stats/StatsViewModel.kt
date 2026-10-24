package com.xichugeek.lunatide.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.*
import com.xichugeek.lunatide.domain.stats.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class StatsUiState(val ready: Boolean = false, val failed: Boolean = false, val today: LocalDate = LocalDate.of(1970, 1, 1), val stats: CycleStats? = null)
@HiltViewModel
class StatsViewModel @Inject constructor(cycles: CycleRepository, logs: DayLogRepository, clock: TodayProvider) : ViewModel() {
    val state = combine(cycles.periods, logs.logs, clock.today) { periods, logs, today ->
        withContext(Dispatchers.Default) { StatsUiState(ready = true, today = today, stats = computeStats(periods, logs, today)) }
    }.catch { error -> if (error is CancellationException) throw error; emit(StatsUiState(failed = true)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState(today = clock.today.value))
}
