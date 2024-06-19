// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.window.layout.WindowMetricsCalculator
import net.leodesouza.blitz.ui.components.BasicTime
import net.leodesouza.blitz.ui.components.LeaningSide
import net.leodesouza.blitz.ui.components.SwipeEdge
import net.leodesouza.blitz.ui.models.BackAction
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration

/**
 * Chess clock screen content consisting of the time of each player in different colors.
 *
 * @param[whiteTimeProvider] Lambda for the remaining time for the first player.
 * @param[blackTimeProvider] Lambda for the remaining time for the second player.
 * @param[clockStateProvider] Lambda for the current state of the clock.
 * @param[playerStateProvider] Lambda for whether the current player is White or Black.
 * @param[leaningSideProvider] Lambda for which side the device is currently leaning towards.
 * @param[backEventActionProvider] Lambda for what action is executed by the back gesture.
 * @param[backEventSwipeEdgeProvider] Lambda for which edge the back gesture starts from.
 * @param[backEventProgressProvider] Lambda for how far along the back gesture is.
 */
@Composable
fun ClockContent(
    whiteTimeProvider: () -> Duration,
    blackTimeProvider: () -> Duration,
    clockStateProvider: () -> ClockState,
    playerStateProvider: () -> PlayerState,
    leaningSideProvider: () -> LeaningSide,
    backEventActionProvider: () -> BackAction,
    backEventSwipeEdgeProvider: () -> SwipeEdge,
    backEventProgressProvider: () -> Float,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val displayOrientation = LocalConfiguration.current.orientation

    // Total window size
    val windowMetricsCalculator = WindowMetricsCalculator.getOrCreate()
    val windowSize = windowMetricsCalculator.computeCurrentWindowMetrics(context).bounds
    val windowWidth = windowSize.width()
    val windowHeight = windowSize.height()

    // Available size
    val windowInsets = WindowInsets.safeDrawing
    val leftPadding = windowInsets.getLeft(density, LayoutDirection.Ltr)
    val topPadding = windowInsets.getTop(density)
    val rightPadding = windowInsets.getRight(density, LayoutDirection.Ltr)
    val bottomPadding = windowInsets.getBottom(density)
    val availableWidth = windowWidth - 2 * max(leftPadding, rightPadding)
    val availableHeight = windowHeight / 2 - 2 * max(bottomPadding, topPadding)

    // Text size
    val simulatedTextPaint = Paint().apply {
        typeface = Typeface.DEFAULT_BOLD
        fontFeatureSettings = "tnum"
    }
    val simulatedTextWidth = simulatedTextPaint.measureText("05:03.0 ")
    val simulatedTextHeight = simulatedTextPaint.fontSpacing
    val simulatedTextSize = simulatedTextPaint.textSize
    val sizeToWidthRatio = simulatedTextSize / simulatedTextWidth
    val sizeToHeightRatio = simulatedTextSize / simulatedTextHeight
    val maxHorizontalTextSize = availableWidth * sizeToWidthRatio
    val maxVerticalTextSize = availableHeight * if (displayOrientation == ORIENTATION_LANDSCAPE) {
        sizeToHeightRatio
    } else {
        sizeToWidthRatio
    }
    val textSize = min(maxHorizontalTextSize, maxVerticalTextSize)

    // Text style
    val textStyle = TextStyle(
        fontSize = with(density) { textSize.toSp() },
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    )
    val timeOverColor = Color.Red
    val oscillatingAlpha by rememberInfiniteTransition(label = "InfiniteTransition").animateFloat(
        initialValue = 1F,
        targetValue = 0.5F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "OscillatingAlpha",
    )

    Column {
        val reusableItemModifier = Modifier
            .weight(1F)
            .fillMaxSize()
            .wrapContentSize()
        BasicTime(
            timeProvider = blackTimeProvider,
            modifier = Modifier
                .background(Color.Black)
                .graphicsLayer {
                    setBasicTimeGraphics(
                        isPlaying = playerStateProvider() == PlayerState.BLACK,
                        windowWidth = windowWidth,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        backEventProgress = backEventProgressProvider(),
                        displayOrientation = displayOrientation,
                    )
                }
                .then(reusableItemModifier),
            style = textStyle.merge(color = Color.White),
            timeOverColor = timeOverColor,
        )
        BasicTime(
            timeProvider = whiteTimeProvider,
            modifier = Modifier
                .background(Color.White)
                .graphicsLayer {
                    setBasicTimeGraphics(
                        isPlaying = playerStateProvider() == PlayerState.WHITE,
                        windowWidth = windowWidth,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        backEventProgress = backEventProgressProvider(),
                        displayOrientation = displayOrientation,
                    )
                }
                .then(reusableItemModifier),
            style = textStyle.merge(color = Color.Black),
            timeOverColor = timeOverColor,
        )
    }
}

/**
 * Set the rotation, translation and opacity of a BasicTime element in a graphics layer scope.
 *
 * @param[isPlaying] Whether the player is currently playing.
 * @param[windowWidth] Current window width in pixels.
 * @param[currentlyAdjustedAlpha] Opacity of the text if the time can currently be adjusted.
 * @param[clockState] Current state of the clock.
 * @param[leaningSide] Which side the device is currently leaning towards.
 * @param[backEventAction] What action is executed by the back gesture.
 * @param[backEventSwipeEdge] Which edge the back gesture starts from.
 * @param[backEventProgress] How far along the back gesture is.
 * @param[displayOrientation] The [ORIENTATION_PORTRAIT] or [ORIENTATION_LANDSCAPE] of the display.
 */
private fun GraphicsLayerScope.setBasicTimeGraphics(
    isPlaying: Boolean,
    windowWidth: Int,
    currentlyAdjustedAlpha: Float,
    clockState: ClockState,
    leaningSide: LeaningSide,
    backEventAction: BackAction,
    backEventSwipeEdge: SwipeEdge,
    backEventProgress: Float,
    displayOrientation: Int,  // ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
) {
    if (displayOrientation == ORIENTATION_PORTRAIT) {
        rotationZ = when (leaningSide) {
            LeaningSide.LEFT -> 90F
            LeaningSide.RIGHT -> -90F
        }
    }

    if (isPlaying || backEventAction != BackAction.PAUSE) {
        translationX = when (backEventSwipeEdge) {
            SwipeEdge.RIGHT -> -backEventProgress * windowWidth
            SwipeEdge.LEFT -> backEventProgress * windowWidth
        }
    }

    if (isPlaying && clockState == ClockState.PAUSED) {
        alpha = currentlyAdjustedAlpha
    }
}
