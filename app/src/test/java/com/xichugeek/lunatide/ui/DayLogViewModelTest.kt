package com.xichugeek.lunatide.ui

import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.repository.DayLogRepository
import com.xichugeek.lunatide.domain.model.*
import com.xichugeek.lunatide.domain.usecase.*
import com.xichugeek.lunatide.ui.calendar.calendarDays
import com.xichugeek.lunatide.ui.daylog.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import com.xichugeek.lunatide.domain.model.Flow as MenstrualFlow

@OptIn(ExperimentalCoroutinesApi::class)
class DayLogViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val day = LocalDate.of(2026, 10, 24)
    private val clock = object : TodayProvider {
        override val today = MutableStateFlow(day)
        override fun refresh() = Unit
        override fun timestamp() = 1L
    }
    private val cycles = FakeCycles { day }
    private val logs = FakeDayRepository(cycles, day)
    private lateinit var vm: DayLogViewModel
    @Before fun setup() { Dispatchers.setMain(dispatcher); vm = DayLogViewModel(logs, cycles, clock) }
    @After fun cleanup() { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    private fun open(date: LocalDate = day) { vm.open(date); scheduler.runCurrent(); assertFalse(vm.state.value?.loading ?: true) }
    private fun advance(ms: Long = 300) { scheduler.advanceTimeBy(ms); scheduler.runCurrent() }

    @Test fun changesCoalesceAfter300ms() {
        open(); vm.setNote("Fictional"); vm.setMood(Mood.CALM); advance(299); assertEquals(0, logs.saves)
        advance(1); assertEquals(1, logs.saves); assertEquals(Mood.CALM, logs.logs.value.single().mood)
    }
    @Test fun closingFlushesBeforeDebounce() {
        open(); vm.setNote("Fictional"); vm.close(); scheduler.runCurrent()
        assertNull(vm.state.value); assertEquals("Fictional", logs.logs.value.single().note); assertEquals(1, logs.saves)
    }
    @Test fun saveFailureKeepsDraftVisible() {
        open(); logs.fail = true; vm.setNote("Draft"); vm.close(); scheduler.runCurrent()
        assertNotNull(vm.state.value); assertTrue(vm.state.value?.failed == true); assertEquals("Draft", vm.state.value?.log?.note)
    }
    @Test fun futureDateIsReadOnly() {
        open(day.plusDays(1)); vm.setNote("Ignored"); vm.setFlow(MenstrualFlow.MEDIUM); vm.setPeriod(true); advance()
        assertTrue(vm.state.value?.future == true); assertNull(vm.state.value?.log?.note); assertEquals(0, logs.saves)
    }
    @Test fun invalidTemperatureDoesNotSaveAndValidValueUsesCentiDegrees() {
        open(); vm.setTemperature("99.2"); vm.close(); scheduler.runCurrent(); assertNotNull(vm.state.value); assertEquals(0, logs.saves)
        vm.setTemperature("36.52"); advance(); assertEquals(3652, logs.logs.value.single().bbtCentiCelsius)
    }
    @Test fun nonPeriodFlowRequiresConfirmationAndAtomicInclusion() {
        open(); vm.setFlow(MenstrualFlow.HEAVY); assertEquals(EditorPrompt.INCLUDE_FOR_FLOW, vm.state.value?.prompt)
        assertTrue(cycles.periods.value.isEmpty()); vm.confirm(); scheduler.runCurrent()
        assertEquals(1, cycles.periods.value.size); assertEquals(MenstrualFlow.HEAVY, logs.logs.value.single().flow)
    }
    @Test fun cancelledFlowLeavesOriginalRecord() {
        open(); vm.setFlow(MenstrualFlow.MEDIUM); vm.cancel(); advance(); assertNull(vm.state.value?.log?.flow); assertEquals(0, logs.saves)
    }
    @Test fun spottingInRecentGapDoesNotCreateNewPeriod() {
        cycles.periods.value = listOf(Period(start = day.minusDays(8), end = day.minusDays(4)))
        open(); vm.setFlow(MenstrualFlow.MEDIUM); vm.confirm(); scheduler.runCurrent()
        assertEquals(EditorPrompt.RECENT, vm.state.value?.prompt); vm.spotting(); advance()
        assertEquals(1, cycles.periods.value.size); assertEquals(MenstrualFlow.SPOTTING, logs.logs.value.single().flow)
    }
    @Test fun middleRemovalConfirmsAndShortensWithoutSplit() {
        cycles.periods.value = listOf(Period(start = day.minusDays(5), end = day))
        open(day.minusDays(2)); vm.setPeriod(false); scheduler.runCurrent(); assertEquals(EditorPrompt.SHORTEN, vm.state.value?.prompt)
        vm.confirm(); scheduler.runCurrent(); assertEquals(day.minusDays(3), cycles.periods.value.single().end)
    }
    @Test fun clearRecordKeepsPeriodDates() {
        cycles.periods.value = listOf(Period(start = day, end = null))
        open(); vm.setNote("Example"); advance(); vm.clear(); vm.confirm(); advance()
        assertTrue(vm.state.value?.log?.isEmpty == true); assertEquals(1, cycles.periods.value.size)
    }
    @Test fun mondayGridContainsLeapDayAndSixWeeks() {
        val cells = calendarDays(YearMonth.of(2028, 2), DayOfWeek.MONDAY)
        assertEquals(42, cells.size); assertEquals(DayOfWeek.MONDAY, cells.first().dayOfWeek); assertTrue(LocalDate.of(2028, 2, 29) in cells)
    }
    @Test fun sundayGridSpansNewYear() {
        val cells = calendarDays(YearMonth.of(2027, 1), DayOfWeek.SUNDAY)
        assertEquals(DayOfWeek.SUNDAY, cells.first().dayOfWeek); assertEquals(2026, cells.first().year); assertEquals(2027, cells.last().year)
    }
}

private class FakeDayRepository(private val cycles: FakeCycles, private val today: LocalDate) : DayLogRepository {
    override val logs = MutableStateFlow<List<DayLog>>(emptyList())
    var saves = 0
    var fail = false
    override suspend fun save(log: DayLog): Result<Unit> {
        require(log.valid(today))
        if (fail) return Result.failure(IllegalStateException("Synthetic error"))
        logs.value = logs.value.filterNot { it.day == log.day } + log; saves++
        return Result.success(Unit)
    }
    override suspend fun editDay(log: DayLog, periodChange: Boolean?, recent: Boolean, shorten: Boolean): Result<PeriodEdit?> {
        val result = periodChange?.let { cycles.edit(if (it) PeriodAction.INCLUDE else PeriodAction.EXCLUDE, log.day, recent, shorten, null).getOrThrow() }
        if (result is PeriodEdit.Rejected) return Result.success(result)
        val normalized = if (periodChange == false) log.copy(flow = null) else log
        save(normalized).getOrThrow(); return Result.success(result)
    }
}
