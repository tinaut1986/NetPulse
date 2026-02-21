package com.tinaut1986.wifitools.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.wifitools.ui.theme.PrimaryBlue
import com.tinaut1986.wifitools.ui.theme.PrimaryPurple
import com.tinaut1986.wifitools.ui.theme.SignalGreen
import com.tinaut1986.wifitools.ui.theme.SignalRed
import com.tinaut1986.wifitools.ui.theme.SignalYellow

@Composable
fun SignalGraph(
    history: List<Int>,
    modifier: Modifier = Modifier
) {
    val animateX = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    
    LaunchedEffect(history.size) {
        animateX.animateTo(1f, animationSpec = tween(500))
    }

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(200.dp)) {
        
        val width = size.width
        val height = size.height
        val maxHistory = 30
        val stepX = width / (maxHistory - 1)
        
        // --- DRAW THRESHOLD LINES ---
        val thresholds = listOf(
            Triple(-40, "Excellent", SignalGreen),
            Triple(-60, "Good", SignalYellow),
            Triple(-80, "Weak", SignalRed)
        )

        thresholds.forEach { (dbm, label, color) ->
            val normalizedY = ((dbm + 100).coerceIn(0, 80)) / 80f
            val y = height - (normalizedY * height)
            
            // Dashed line
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            // Label
            drawText(
                textMeasurer = textMeasurer,
                text = "$dbm dBm ($label)",
                style = TextStyle(color = color.copy(alpha = 0.5f), fontSize = 10.sp),
                topLeft = Offset(8.dp.toPx(), y - 16.dp.toPx())
            )
        }

        if (history.isEmpty()) return@Canvas

        val path = Path()
        
        history.takeLast(maxHistory).forEachIndexed { index, rssi ->
            // Normalize RSSI to height (e.g. -100 to -20)
            val normalizedRssi = ((rssi + 100).coerceIn(0, 80)) / 80f
            val x = index * stepX
            val y = height - (normalizedRssi * height)
            
            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryPurple)),
            style = Stroke(width = 4.dp.toPx())
        )
        
        // Add a subtle fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(PrimaryBlue.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
    }
}
