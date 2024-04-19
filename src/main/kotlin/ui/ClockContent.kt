// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.BackEventCompat
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.leodesouza.blitz.ui.components.BasicTime
import net.leodesouza.blitz.ui.components.LeaningSide
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Chess clock screen content consisting of the time of each player in different colors.
 *
 * @param[whiteTimeProvider] Lambda for the remaining time for the first player.
 * @param[blackTimeProvider] Lambda for the remaining time for the second player.
 * @param[clockStateProvider] Lambda for the current state of the clock.
 * @param[playerStateProvider] Lambda for whether the current player is White or Black.
 * @param[leaningSideProvider] Lambda for which side the device is currently leaning towards.
 * @param[backEventActionProvider] Lambda for what action is executed by the back gesture.
 * @param[backEventProgressProvider] Lambda for the progress of the back gesture.
 * @param[backEventSwipeEdgeProvider] Lambda for the swipe edge where the back gesture starts.
 */
@Composable
fun ClockContent(
    whiteTimeProvider: () -> Duration,
    blackTimeProvider: () -> Duration,
    clockStateProvider: () -> ClockState,
    playerStateProvider: () -> PlayerState,
    leaningSideProvider: () -> LeaningSide,
    backEventActionProvider: () -> ClockBackAction,
    backEventProgressProvider: () -> Float,
    backEventSwipeEdgeProvider: () -> Int,
) {
    val displayOrientation = LocalConfiguration.current.orientation
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    val textHeight = screenHeight / if (displayOrientation == ORIENTATION_LANDSCAPE) 3 else 8
    val fontSize = with(density) { textHeight.toSp() }
    val fontWeight = FontWeight.Bold
    val timeOverColor = Color.Red

    val infiniteTransition = rememberInfiniteTransition(label = "OscillatingAlphaTransition")
    val oscillatingAlpha by infiniteTransition.animateFloat(
        initialValue = 1F,
        targetValue = 0.5F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "OscillatingAlphaAnimation",
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
                        screenWidth = screenWidth,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventProgress = backEventProgressProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        displayOrientation = displayOrientation,
                    )
                }
                .then(reusableItemModifier),
            style = TextStyle(color = Color.White, fontSize = fontSize, fontWeight = fontWeight),
            timeOverColor = timeOverColor,
        )
        BasicTime(
            timeProvider = whiteTimeProvider,
            modifier = Modifier
                .background(Color.White)
                .graphicsLayer {
                    setBasicTimeGraphics(
                        isPlaying = playerStateProvider() == PlayerState.WHITE,
                        screenWidth = screenWidth,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventProgress = backEventProgressProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        displayOrientation = displayOrientation,
                    )
                }
                .then(reusableItemModifier),
            style = TextStyle(color = Color.Black, fontSize = fontSize, fontWeight = fontWeight),
            timeOverColor = timeOverColor,
        )
    }
}

/** Preview the chess clock screen content in Android Studio. */
@Preview
@PreviewScreenSizes
@Composable
private fun ClockContentPreview() {
    ClockContent(
        whiteTimeProvider = { 5.minutes + 3.seconds },
        blackTimeProvider = { 5.minutes + 3.seconds },
        clockStateProvider = { ClockState.FULL_RESET },
        playerStateProvider = { PlayerState.WHITE },
        leaningSideProvider = { LeaningSide.RIGHT },
        backEventActionProvider = { ClockBackAction.PAUSE },
        backEventProgressProvider = { 0F },
        backEventSwipeEdgeProvider = { BackEventCompat.EDGE_LEFT },
    )
}

/**
 * Set the rotation, translation and opacity of a BasicTime element in a graphics layer scope.
 *
 * @param[isPlaying] Whether the player is currently playing.
 * @param[screenWidth] Current width of the screen in the Dp unit.
 * @param[currentlyAdjustedAlpha] Opacity of the text if the time can currently be adjusted.
 * @param[clockState] Current state of the clock.
 * @param[leaningSide] Which side the device is currently leaning towards.
 * @param[backEventAction] What action is executed by the back gesture.
 * @param[backEventProgress] Progress of the back gesture.
 * @param[backEventSwipeEdge] Swipe edge where the back gesture starts.
 * @param[displayOrientation] The `ORIENTATION_PORTRAIT` or [ORIENTATION_LANDSCAPE] of the display.
 */
private fun GraphicsLayerScope.setBasicTimeGraphics(
    isPlaying: Boolean,
    screenWidth: Dp,
    currentlyAdjustedAlpha: Float,
    clockState: ClockState,
    leaningSide: LeaningSide,
    backEventAction: ClockBackAction,
    backEventProgress: Float,
    backEventSwipeEdge: Int,
    displayOrientation: Int,  // ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
) {
    rotationZ = if (displayOrientation == ORIENTATION_LANDSCAPE) {
        0F
    } else when (leaningSide) {
        LeaningSide.LEFT -> 90F
        LeaningSide.RIGHT -> -90F
    }

    translationX = if (backEventAction == ClockBackAction.PAUSE && !isPlaying) {
        0F
    } else {
        val sign = if (backEventSwipeEdge == BackEventCompat.EDGE_RIGHT) -1F else 1F
        sign * backEventProgress * screenWidth.toPx()
    }

    alpha = if (clockState == ClockState.PAUSED && isPlaying) {
        currentlyAdjustedAlpha
    } else {
        1F
    }
}
