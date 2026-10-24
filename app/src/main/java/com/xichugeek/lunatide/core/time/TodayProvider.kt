package com.xichugeek.lunatide.core.time

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface TodayProvider {
    val today: StateFlow<LocalDate>
    fun refresh()
    fun timestamp(): Long
}

@Singleton
class SystemTodayProvider @Inject constructor() : TodayProvider {
    private val current = MutableStateFlow(LocalDate.now())
    override val today: StateFlow<LocalDate> = current
    override fun refresh() { current.value = LocalDate.now() }
    override fun timestamp() = System.currentTimeMillis()
}
