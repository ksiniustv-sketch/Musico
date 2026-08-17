package com.Ksinius.musico.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.Ksinius.musico.model.ThemeSettings
import com.Ksinius.musico.ui.theme.PureBlack
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun BubbleBackground(
    modifier: Modifier = Modifier,
    bubbleCount: Int = 18,
    themeSettings: ThemeSettings = ThemeSettings()
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PureBlack,
                        PureBlack,
                        PureBlack,
                        PureBlack
                    )
                )
            )
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        repeat(bubbleCount) { index ->
            RisingBubble(
                index = index,
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                themeSettings = themeSettings
            )
        }
    }
}

@Composable
private fun RisingBubble(
    index: Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    themeSettings: ThemeSettings
) {
    val random = remember(index) { Random(index + 17) }
    val density = LocalDensity.current

    val bubbleSizeDp = remember { random.nextInt(14, 52).dp }
    val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }
    val startXFraction = remember { random.nextFloat() }
    val durationMs = remember { random.nextInt(7000, 14000) }
    val startDelayMs = remember { random.nextInt(0, 9000) }
    val horizontalDriftPx = remember { random.nextFloat() * 80f - 40f }
    val peakAlpha = remember { random.nextFloat() * 0.28f + 0.22f }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(containerHeightPx, containerWidthPx) {
        delay(startDelayMs.toLong())
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs)
            )
            delay(random.nextLong(400, 2200))
        }
    }

    val t = progress.value
    val yPx = containerHeightPx * (1f - t) - bubbleSizePx
    val xPx = (startXFraction * (containerWidthPx - bubbleSizePx)).coerceAtLeast(0f) +
            horizontalDriftPx * t

    val alpha = when {
        t < 0.12f -> (t / 0.12f) * peakAlpha
        t > 0.82f -> ((1f - t) / 0.18f) * peakAlpha
        else -> peakAlpha
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(bubbleSizeDp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        themeSettings.toLighterColor().copy(alpha = 0.75f),
                        themeSettings.toColor().copy(alpha = 0.4f),
                        themeSettings.toDarkerColor().copy(alpha = 0.12f)
                    )
                )
            )
    )
}
