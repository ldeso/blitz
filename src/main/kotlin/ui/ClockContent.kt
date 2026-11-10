// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import net.leodesouza.blitz.ui.components.BasicTime
import net.leodesouza.blitz.ui.components.LeaningSide
import net.leodesouza.blitz.ui.components.SwipeEdge
import net.leodesouza.blitz.ui.models.BackAction
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
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
    val displayOrientation = LocalConfiguration.current.orientation

    // Symmetric padding
    val windowInsets = WindowInsets.safeDrawing
    val paddingValues = windowInsets.asPaddingValues()
    val leftPadding = paddingValues.calculateLeftPadding(LayoutDirection.Ltr)
    val rightPadding = paddingValues.calculateRightPadding(LayoutDirection.Ltr)
    val topPadding = paddingValues.calculateTopPadding()
    val bottomPadding = paddingValues.calculateBottomPadding()
    val symmetricPaddingValues = PaddingValues(
        horizontal = max(leftPadding, rightPadding),
        vertical = max(bottomPadding, topPadding),
    )

    // Text style
    val textStyle = TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
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
    val isTextVertical = (displayOrientation == ORIENTATION_PORTRAIT)

    Column {
        val reusableItemModifier = Modifier
            .weight(1F)
            .fillMaxSize()
        BoxWithConstraints(
            modifier = Modifier
                .background(Color.Black)
                .padding(symmetricPaddingValues)
                .graphicsLayer {
                    setBasicTimeGraphics(
                        isPlaying = playerStateProvider() == PlayerState.BLACK,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        backEventProgress = backEventProgressProvider(),
                        isTextVertical = isTextVertical,
                    )
                }
                .then(reusableItemModifier),
            contentAlignment = Alignment.Center,
        ) {
            BasicTime(
                timeProvider = blackTimeProvider,
                modifier = Modifier.wrapContentSize(unbounded = true),
                style = textStyle.merge(
                    color = Color.White,
                    fontSize = fittingFontSize(textStyle, constraints, isTextVertical),
                ),
                timeOverColor = timeOverColor,
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .background(Color.White)
                .padding(symmetricPaddingValues)
                .graphicsLayer {
                    setBasicTimeGraphics(
                        isPlaying = playerStateProvider() == PlayerState.WHITE,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        backEventProgress = backEventProgressProvider(),
                        isTextVertical = isTextVertical,
                    )
                }
                .then(reusableItemModifier),
            contentAlignment = Alignment.Center,
        ) {
            BasicTime(
                timeProvider = whiteTimeProvider,
                modifier = Modifier.wrapContentSize(unbounded = true),
                style = textStyle.merge(
                    color = Color.Black,
                    fontSize = fittingFontSize(textStyle, constraints, isTextVertical),
                ),
                timeOverColor = timeOverColor,
            )
        }
    }
}

/**
 * Set the rotation, translation and opacity of a BasicTime element in a graphics layer scope.
 *
 * @param[isPlaying] Whether the player is currently playing.
 * @param[currentlyAdjustedAlpha] Opacity of the text if the time can currently be adjusted.
 * @param[clockState] Current state of the clock.
 * @param[leaningSide] Which side the device is currently leaning towards.
 * @param[backEventAction] What action is executed by the back gesture.
 * @param[backEventSwipeEdge] Which edge the back gesture starts from.
 * @param[backEventProgress] How far along the back gesture is.
 * @param[isTextVertical] Whether the text should be rotated to be vertical.
 */
private fun GraphicsLayerScope.setBasicTimeGraphics(
    isPlaying: Boolean,
    currentlyAdjustedAlpha: Float,
    clockState: ClockState,
    leaningSide: LeaningSide,
    backEventAction: BackAction,
    backEventSwipeEdge: SwipeEdge,
    backEventProgress: Float,
    isTextVertical: Boolean,
) {
    if (isTextVertical) {
        rotationZ = when (leaningSide) {
            LeaningSide.LEFT -> 90F
            LeaningSide.RIGHT -> -90F
        }
    }

    if (isPlaying || backEventAction != BackAction.PAUSE) {
        translationX = when (backEventSwipeEdge) {
            SwipeEdge.RIGHT -> -backEventProgress * size.width
            SwipeEdge.LEFT -> backEventProgress * size.width
        }
    }

    if (isPlaying && clockState == ClockState.PAUSED) {
        alpha = currentlyAdjustedAlpha
    }
}

/**
 * Get the biggest font size at which any time displayed by [BasicTime] fits in the given
 * [constraints] with the given [style], taking into account whether the text [isVertical].
 */
@Composable
private fun fittingFontSize(
    style: TextStyle,
    constraints: Constraints,
    isVertical: Boolean,
): TextUnit {
    val referenceFontSize = 100.sp
    val textMeasurer = rememberTextMeasurer()
    val referenceSize = remember(textMeasurer, style) {
        val referenceStyle = style.merge(fontSize = referenceFontSize)
        val sizes = listOf("00:00.0", "0:00:00").map {
            textMeasurer.measure(it, referenceStyle).size
        }
        IntSize(width = sizes.maxOf { it.width }, height = sizes.maxOf { it.height })
    }
    val (width, height) = if (isVertical) {
        constraints.maxHeight to constraints.maxWidth
    } else {
        constraints.maxWidth to constraints.maxHeight
    }
    val scale = minOf(
        width.toFloat() / referenceSize.width,
        height.toFloat() / referenceSize.height,
    )

    return with(LocalDensity.current) { (referenceFontSize.toPx() * scale).toSp() }
}
