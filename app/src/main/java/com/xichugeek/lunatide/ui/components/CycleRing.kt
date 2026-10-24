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
import kotlin.math.*

@Composable
fun CycleRing(cycleDay: Int?, cycleLength: Int, periodLength: Int, showFertility: Boolean, late: Boolean, description: String, content: @Composable () -> Unit) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val moon = MaterialTheme.colorScheme.primary
    Box(Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f).clearAndSetSemantics { contentDescription = description }) {
        Canvas(Modifier.matchParentSize().padding(12.dp).clearAndSetSemantics { }) {
            val stroke = 10.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            val offset = Offset(stroke, stroke)
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            drawCircle(track, radius, style = Stroke(stroke))
            drawArc(Color(0xFFE0637A), -90f, 360f * periodLength / cycleLength, false, offset, arcSize, style = Stroke(stroke))
            if (showFertility) drawArc(Color(0xFF4FA3A5), -90f + 360f * (cycleLength - 19) / cycleLength, 360f * 7 / cycleLength, false, offset, arcSize, style = Stroke(stroke))
            if (late) drawCircle(Color(0xFFE0637A), radius - stroke * 1.4f, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            val angle = (360.0 * ((cycleDay ?: 1) - 1).coerceAtLeast(0) / cycleLength - 90) * PI / 180
            drawCircle(moon, 7.dp.toPx(), Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius))
        }
        Box(Modifier.matchParentSize().padding(42.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { content() }
    }
}
