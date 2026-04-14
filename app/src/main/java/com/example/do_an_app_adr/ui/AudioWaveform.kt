package com.example.do_an_app_adr.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = false
) {
    val barColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val animates = List(15) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + (index * 50),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Canvas(modifier = modifier.height(40.dp).fillMaxWidth()) {
        val width = size.width
        val height = size.height
        val barCount = animates.size
        val barWidth = 4.dp.toPx()
        val gap = 4.dp.toPx()
        val totalBarWidth = barWidth + gap
        
        val startX = (width - (barCount * totalBarWidth)) / 2

        for (i in 0 until barCount) {
            val progress = if (isAnimating) animates[i].value else 0.3f
            val barHeight = height * progress
            val x = startX + i * totalBarWidth
            
            drawLine(
                color = barColor,
                start = Offset(x, (height - barHeight) / 2),
                end = Offset(x, (height + barHeight) / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
