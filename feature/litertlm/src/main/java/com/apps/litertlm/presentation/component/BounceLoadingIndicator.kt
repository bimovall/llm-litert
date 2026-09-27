package com.apps.litertlm.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BounceLoadingIndicator(modifier: Modifier = Modifier, bounceHeight: Dp = 4.dp, dotSize: Dp = 4.dp,) {
    val infiniteTransition = rememberInfiniteTransition()

    val dots = listOf(0, 100, 200)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(dotSize + bounceHeight),
    ) {
        dots.forEach { it ->
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0F,
                targetValue = 1F,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(it)
                ),
                label = "bouncing_$it"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer { translationY = -yOffset * bounceHeight.toPx() }
                    .background(Color.Black.copy(alpha = 0.8F), shape = CircleShape)
            )
        }
    }
}