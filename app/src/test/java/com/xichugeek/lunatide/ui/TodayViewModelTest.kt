package com.xichugeek.lunatide.ui

import androidx.lifecycle.viewModelScope

import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.*
import com.xichugeek.lunatide.data.settings.*
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.*
import com.xichugeek.lunatide.ui.onboarding.OnboardingViewModel
import com.xichugeek.lunatide.ui.today.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val date = LocalDate.of(2026, 10, 24)
    private val clock = object : TodayProvider {
        override val today = MutableStateFlow(date)
        override fun refresh() = Unit
        override fun timestamp() = 1L
    }
    private val cycles = FakeCycles { clock.today.value }
    private val logs = FakeLogs()
    private val settings = FakeSettings()
    private val models = mutableListOf<androidx.lifecycle.ViewModel>()
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun cleanup() { models.forEach { it.viewModelScope.cancel() }; Dispatchers.resetMain() }
    private suspend fun model(): TodayViewModel = TodayViewModel(cycles, logs, settings, clock).also { models += it; await(it) { state -> state.ready } }
    private suspend fun await(vm: TodayViewModel, predicate: (TodayUiState) -> Boolean): TodayUiState = withTimeout(5000) { vm.state.first(predicate) }

    @Test fun emptyStateIsWelcome() = runBlocking { val vm = model(); assertEquals(HomeStage.WELCOME, vm.state.value.homeStatus().stage) }
    @Test fun startCreatesDayOneAndUndoRestores() = runBlocking {
        val vm = model(); vm.record(); val state = await(vm) { it.periods.isNotEmpty() }
        assertEquals(HomeStage.ONGOING, state.homeStatus().stage); assertEquals(1, state.homeStatus().days)
        vm.undo(); await(vm) { it.periods.isEmpty() }; assertTrue(cycles.periods.value.isEmpty())
    }
    @Test fun endSwitchesTodayToWaitingAndUndoReopens() = runBlocking {
        cycles.periods.value = listOf(Period(start = date.minusDays(4), end = null))
        val vm = model(); vm.record(); val state = await(vm) { it.periods.singleOrNull()?.end != null }
        assertEquals(HomeStage.WAITING, state.homeStatus().stage)
        vm.undo(); await(vm) { it.periods.singleOrNull()?.end == null }; assertNull(cycles.periods.value.single().end)
    }
    @Test fun recentPeriodRequiresConfirmationAndSpottingDoesNotCreatePeriod() = runBlocking {
        cycles.periods.value = listOf(Period(start = date.minusDays(8), end = date.minusDays(4)))
        val vm = model(); vm.record(); assertEquals(EditIssue.CONFIRM_RECENT, vm.actions.value.issue)
        vm.spotting(); assertEquals(1, cycles.periods.value.size)
        assertEquals(com.xichugeek.lunatide.domain.model.Flow.SPOTTING, logs.logs.value.single().flow)
    }
    @Test fun recentConfirmationCreatesNewOngoing() = runBlocking {
        cycles.periods.value = listOf(Period(start = date.minusDays(8), end = date.minusDays(4)))
        val vm = model(); vm.record(); vm.record(recent = true); await(vm) { it.periods.size == 2 }
        assertNull(cycles.periods.value.last().end)
    }
    @Test fun reviseStartUsesOriginalSnapshot() = runBlocking {
        val vm = model(); vm.record(); await(vm) { it.periods.isNotEmpty() }
        vm.record(date.minusDays(2), revise = true); await(vm) { it.periods.singleOrNull()?.start == date.minusDays(2) }
        assertEquals(1, cycles.periods.value.size)
    }
    @Test fun futureDateRejectedWithoutChanges() = runBlocking {
        val vm = model(); vm.record(date.plusDays(1)); assertEquals(EditIssue.FUTURE_DATE, vm.actions.value.issue); assertTrue(cycles.periods.value.isEmpty())
    }
    @Test fun dueAndLateStatesUseOriginalNextDate() = runBlocking {
        cycles.periods.value = listOf(Period(start = date.minusDays(28), end = date.minusDays(24)))
        val vm = model(); assertEquals(HomeStage.DUE, vm.state.value.homeStatus().stage)
        clock.today.value = date.plusDays(4)
        val state = await(vm) { it.today == date.plusDays(4) }; assertEquals(HomeStage.LATE, state.homeStatus().stage); assertEquals(4, state.homeStatus().days)
    }
    @Test fun onboardingSkipDoesNotCreateHealthRecord() = runBlocking {
        val vm = OnboardingViewModel(cycles, settings, clock).also { models += it }
        vm.finish(); assertTrue(settings.settings.value.onboardingCompleted); assertTrue(cycles.periods.value.isEmpty())
        assertNull(settings.settings.value.initialCycleLength)
    }
    @Test fun onboardingHistoricalRecordUsesChosenPeriodLength() = runBlocking {
        val vm = OnboardingViewModel(cycles, settings, clock).also { models += it }
        vm.finish(date.minusDays(10), 32, 6)
        assertEquals(date.minusDays(5), cycles.periods.value.single().end)
        assertEquals(32, settings.settings.value.initialCycleLength)
    }
    @Test fun onboardingEndTodayRemainsOngoing() = runBlocking {
        val vm = OnboardingViewModel(cycles, settings, clock).also { models += it }
        vm.finish(date.minusDays(4), null, 5); assertNull(cycles.periods.value.single().end)
    }
    @Test fun repositoryFailureSurfacesWithoutSuccessfulNotice() = runBlocking {
        val vm = model(); cycles.fail = true; vm.record(); assertTrue(vm.actions.value.storageError); assertNull(vm.actions.value.notice)
    }
}

class FakeSettings : SettingsStore {
    override val settings = MutableStateFlow(AppSettings())
    override suspend fun update(transform: (AppSettings) -> AppSettings) { settings.value = transform(settings.value) }
    override suspend fun clear() { settings.value = AppSettings() }
}
class FakeCycles(private val today: () -> LocalDate) : CycleRepository {
    override val periods = MutableStateFlow<List<Period>>(emptyList())
    var fail = false
    override suspend fun edit(action: PeriodAction, day: LocalDate, recent: Boolean, shorten: Boolean, base: List<Period>?): Result<PeriodEdit> {
        if (fail) return Result.failure(IllegalStateException("Synthetic failure"))
        val result = PeriodRules.edit(base ?: periods.value, action, day, today(), recent, shorten)
        if (result is PeriodEdit.Applied) periods.value = result.periods
        return Result.success(result)
    }
    override suspend fun restorePeriods(periods: List<Period>): Result<Unit> { this.periods.value = periods; return Result.success(Unit) }
}
class FakeLogs : DayLogRepository {
    override val logs = MutableStateFlow<List<DayLog>>(emptyList())
    override suspend fun save(log: DayLog): Result<Unit> { logs.value = logs.value.filterNot { it.day == log.day } + log; return Result.success(Unit) }
}
