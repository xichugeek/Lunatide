package com.xichugeek.lunatide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** 周期语义色集中维护；日期圆底使用满足小字对比度的较深玫瑰色。 */
@Immutable
data class CycleColors(
    val period: Color, val periodFill: Color, val onPeriod: Color, val periodContainer: Color,
    val fertile: Color, val fertileContainer: Color, val ovulation: Color, val spotting: Color,
)

private val LightCycleColors = CycleColors(
    Color(0xFFE0637A), Color(0xFFB53E57), Color.White, Color(0xFFFBE3E7),
    Color(0xFF4FA3A5), Color(0xFFDDF1F1), Color(0xFF2E7D80), Color(0xFFC98A96),
)
private val DarkCycleColors = CycleColors(
    Color(0xFFF08C9E), Color(0xFFF08C9E), Color(0xFF35111B), Color(0xFF4A2530),
    Color(0xFF7CC9CB), Color(0xFF1F3D3E), Color(0xFFA5E0E1), Color(0xFFD9A3AD),
)

@Composable
fun cycleColors(): CycleColors =
    if (MaterialTheme.colorScheme.background.luminance() < 0.2f) DarkCycleColors else LightCycleColors
