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

data class OnboardingState(val busy: Boolean = false, val failed: Boolean = false)
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val cycles: CycleRepository, private val settings: SettingsStore, private val clock: TodayProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()
    fun finish(start: LocalDate? = null, cycle: Int? = null, period: Int? = null) {
        if (_state.value.busy) return
        _state.value = OnboardingState(busy = true)
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
            catch (_: Exception) { _state.value = OnboardingState(failed = true) }
        }
    }
}
