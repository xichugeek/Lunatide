package com.xichugeek.lunatide.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.*
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.*

data class CalendarUiState(val month: YearMonth, val weekStart: DayOfWeek = DayOfWeek.MONDAY)
@HiltViewModel
class CalendarViewModel @Inject constructor(private val clock: TodayProvider, settings: SettingsStore) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(clock.today.value))
    val state = combine(month, settings.settings) { month, settings ->
        CalendarUiState(month, if (settings.weekStart == "SUNDAY") DayOfWeek.SUNDAY else DayOfWeek.MONDAY)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CalendarUiState(month.value))
    fun move(delta: Int) { month.value = month.value.plusMonths(delta.toLong()) }
    fun today() { month.value = YearMonth.from(clock.today.value) }
    fun show(day: LocalDate) { month.value = YearMonth.from(day) }
}
fun calendarDays(month: YearMonth, weekStart: DayOfWeek): List<LocalDate> {
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(weekStart))
    return (0 until 42).map { first.plusDays(it.toLong()) }
}
