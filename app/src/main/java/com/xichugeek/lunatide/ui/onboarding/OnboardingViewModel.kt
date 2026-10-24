package com.xichugeek.lunatide.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.CycleRepository
import com.xichugeek.lunatide.data.settings.SettingsStore
import com.xichugeek.lunatide.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class OnboardingDraft(val page: Int = 0, val start: LocalDate? = null, val period: Int = 5, val cycle: Int = 28, val periodKnown: Boolean = false, val cycleKnown: Boolean = false)
data class OnboardingState(val busy: Boolean = false, val failed: Boolean = false, val draft: OnboardingDraft = OnboardingDraft())
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val cycles: CycleRepository, private val settings: SettingsStore, private val clock: TodayProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()
    fun draft(value: OnboardingDraft) {
        if (value.page in 0..4 && value.period in 2..10 && value.cycle in 20..45 && (value.start == null || value.start <= clock.today.value)) _state.update { it.copy(draft = value) }
    }
    fun finish(start: LocalDate? = null, cycle: Int? = null, period: Int? = null) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            try {
                if (start != null) {
                    require(start <= clock.today.value)
                    val result = cycles.edit(PeriodAction.START, start).getOrThrow()
                    require(result is PeriodEdit.Applied || result == PeriodEdit.Rejected(EditIssue.ALREADY_INCLUDED))
                    val end = start.plusDays((period ?: 5) - 1L)
                    if (end < clock.today.value) cycles.edit(PeriodAction.END, end).getOrThrow()
                }
                settings.update { it.copy(onboardingCompleted = true, initialCycleLength = cycle, initialPeriodLength = period) }
                _state.value = OnboardingState()
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _state.update { it.copy(busy = false, failed = true) } }
        }
    }
}
