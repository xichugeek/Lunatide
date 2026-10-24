package com.xichugeek.lunatide.ui

import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.CycleRepository
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.*
import com.xichugeek.lunatide.ui.stats.StatsViewModel
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val date = LocalDate.of(2026, 10, 24)
    private val clock = object : TodayProvider {
        override val today = MutableStateFlow(date)
        override fun refresh() = Unit
        override fun timestamp() = 1L
    }
    private val cycles = FakeCycles { clock.today.value }
    private val logs = FakeLogs()
    private lateinit var vm: StatsViewModel
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun cleanup() { vm.viewModelScope.cancel(); Dispatchers.resetMain() }

    @Test fun emptyHistoryAndLiveUpdates() = runBlocking {
        vm = StatsViewModel(cycles, logs, clock)
        assertTrue(withTimeout(5000) { vm.state.first { it.ready } }.stats?.cycles?.isEmpty() == true)
        cycles.periods.value = listOf(Period(start = date.minusDays(53), end = date.minusDays(49)), Period(start = date.minusDays(24), end = date.minusDays(20)))
        val state = withTimeout(5000) { vm.state.first { it.stats?.cycles?.size == 2 } }
        assertEquals(29.0, state.stats?.avgCycleLength ?: 0.0, 0.001)
        logs.save(DayLog(date.minusDays(24), symptoms = setOf(Symptom.FATIGUE)))
        val updated = withTimeout(5000) { vm.state.first { it.stats?.topSymptoms?.isNotEmpty() == true } }
        assertEquals(Symptom.FATIGUE to 1, updated.stats?.topSymptoms?.first())
    }
    @Test fun changingTodayExcludesFutureHistory() = runBlocking {
        cycles.periods.value = listOf(Period(start = date, end = null))
        vm = StatsViewModel(cycles, logs, clock)
        withTimeout(5000) { vm.state.first { it.ready } }
        clock.today.value = date.minusDays(1)
        val state = withTimeout(5000) { vm.state.first { it.today == date.minusDays(1) && it.ready } }
        assertTrue(state.stats?.cycles?.isEmpty() == true)
    }
    @Test fun unavailableRepositorySurfacesError() = runBlocking {
        val broken = object : CycleRepository {
            override val periods: kotlinx.coroutines.flow.Flow<List<Period>> = flow { throw IllegalStateException("Synthetic failure") }
            override suspend fun edit(action: PeriodAction, day: LocalDate, recent: Boolean, shorten: Boolean, base: List<Period>?): Result<PeriodEdit> = Result.failure(IllegalStateException())
            override suspend fun restorePeriods(periods: List<Period>): Result<Unit> = Result.failure(IllegalStateException())
        }
        vm = StatsViewModel(broken, logs, clock)
        assertTrue(withTimeout(5000) { vm.state.first { it.failed } }.failed)
    }
}
