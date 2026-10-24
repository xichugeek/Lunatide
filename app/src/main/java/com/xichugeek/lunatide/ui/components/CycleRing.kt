package com.xichugeek.lunatide.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.*

@Composable
fun CycleRing(cycleDay: Int?, cycleLength: Int, periodLength: Int, showFertility: Boolean, late: Boolean, description: String, night: Boolean = false, content: @Composable () -> Unit) {
    val track = if (night) Color(0xFF41405F) else MaterialTheme.colorScheme.surfaceVariant
    val moon = if (night) Color(0xFFF4F0FF) else MaterialTheme.colorScheme.primary
    val progress by animateFloatAsState(((cycleDay ?: 1) - 1).coerceAtLeast(0).toFloat(), tween(600), label = "cycleMoon")
    if (LocalDensity.current.fontScale > 1.4f) {
        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp).clearAndSetSemantics { contentDescription = description }, contentAlignment = androidx.compose.ui.Alignment.Center) { content() }
        return
    }
    Box(Modifier.widthIn(max = 248.dp).fillMaxWidth().aspectRatio(1f).clearAndSetSemantics { contentDescription = description }) {
        Canvas(Modifier.matchParentSize().padding(12.dp).clearAndSetSemantics { }) {
            val stroke = 10.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            val offset = Offset(stroke, stroke)
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            drawCircle(track, radius, style = Stroke(stroke))
            drawArc(if (night) Color(0xFFF08C9E) else Color(0xFFE0637A), -90f, 360f * periodLength / cycleLength, false, offset, arcSize, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            if (showFertility) drawArc(if (night) Color(0xFF7CC9CB) else Color(0xFF4FA3A5), -90f + 360f * (cycleLength - 19) / cycleLength, 360f * 7 / cycleLength, false, offset, arcSize, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            if (late) drawCircle(Color(0xFFE0637A), radius - stroke * 1.4f, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            val angle = (360.0 * progress / cycleLength - 90) * PI / 180
            val moonCenter = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            drawCircle(track, 10.dp.toPx(), moonCenter)
            drawCircle(moon, 6.dp.toPx(), moonCenter)
            if ((cycleDay ?: 1) % cycleLength < cycleLength / 2) drawCircle(track, 5.dp.toPx(), moonCenter + Offset(3.dp.toPx(), -2.dp.toPx()))
        }
        Box(Modifier.matchParentSize().padding(42.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { content() }
    }
}
