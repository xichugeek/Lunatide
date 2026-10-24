package com.xichugeek.lunatide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import com.xichugeek.lunatide.ui.LunatideRoot
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import com.xichugeek.lunatide.ui.today.TodayViewModel
import com.xichugeek.lunatide.ui.onboarding.OnboardingViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val today: TodayViewModel by viewModels()
    private val onboarding: OnboardingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by today.state.collectAsState()
            val actions by today.actions.collectAsState()
            val onboardingState by onboarding.state.collectAsState()
            val dark = when (state.settings.theme) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
            LunatideTheme(dark) {
                LunatideRoot(state, actions, onboardingState, onboarding::finish,
                    { date, recent, revise -> today.record(date, recent, revise) }, today::undo, today::spotting, today::clearMessage, today::retry)
            }
        }
    }
    override fun onResume() { super.onResume(); today.refresh() }
}
