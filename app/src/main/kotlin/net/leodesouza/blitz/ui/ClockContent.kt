/*
 * Copyright 2024 Léo de Souza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.leodesouza.blitz.ui

import android.content.res.Configuration
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.leodesouza.blitz.ui.components.BasicTime
import net.leodesouza.blitz.ui.components.LeaningSide

/**
 * Chess clock screen content consisting of the time of each player in different colors.
 *
 * @param[whiteTimeProvider] Lambda for the remaining time for the first player.
 * @param[blackTimeProvider] Lambda for the remaining time for the second player.
 * @param[isWhiteTurnProvider] Lambda for whether it is the turn of the first or the second player.
 * @param[isStartedProvider] Lambda for whether the clock has started ticking.
 * @param[isPausedProvider] Lambda for whether the clock is on pause.
 * @param[leaningSideProvider] Lambda for which side the device is currently leaning towards.
 * @param[backEventActionProvider] Lambda for what action is executed by the back gesture.
 * @param[backEventProgressProvider] Lambda for the progress of the back gesture.
 * @param[backEventSwipeEdgeProvider] Lambda for the swipe edge where the back gesture starts.
 */
@Composable
fun ClockContent(
    whiteTimeProvider: () -> Long,
    blackTimeProvider: () -> Long,
    isWhiteTurnProvider: () -> Boolean,
    isStartedProvider: () -> Boolean,
    isPausedProvider: () -> Boolean,
    leaningSideProvider: () -> LeaningSide,
    backEventActionProvider: () -> ClockBackAction,
    backEventProgressProvider: () -> Float,
    backEventSwipeEdgeProvider: () -> Int,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    val textHeight = screenHeight / if (isLandscape) 3 else 8
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
                        screenWidth = screenWidth,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        isPlayerTurn = !isWhiteTurnProvider(),
                        isStarted = isStartedProvider(),
                        isPaused = isPausedProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventProgress = backEventProgressProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        isLandscape = isLandscape,
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
                        screenWidth = screenWidth,
                        currentlyAdjustedAlpha = oscillatingAlpha,
                        isPlayerTurn = isWhiteTurnProvider(),
                        isStarted = isStartedProvider(),
                        isPaused = isPausedProvider(),
                        leaningSide = leaningSideProvider(),
                        backEventAction = backEventActionProvider(),
                        backEventProgress = backEventProgressProvider(),
                        backEventSwipeEdge = backEventSwipeEdgeProvider(),
                        isLandscape = isLandscape,
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
@Composable
private fun ClockContentPreview() {
    ClockContent(
        whiteTimeProvider = { 5L * 60_000L },
        blackTimeProvider = { 3L * 1_000L },
        isWhiteTurnProvider = { true },
        leaningSideProvider = { LeaningSide.RIGHT },
        isStartedProvider = { false },
        isPausedProvider = { true },
        backEventActionProvider = { ClockBackAction.PAUSE },
        backEventProgressProvider = { 0F },
        backEventSwipeEdgeProvider = { BackEventCompat.EDGE_LEFT },
    )
}

/**
 * Set the rotation, translation and opacity of a BasicTime element in a graphics layer scope.
 *
 * @param[screenWidth] Current width of the screen in the Dp unit.
 * @param[currentlyAdjustedAlpha] Opacity of the text if the time can currently be adjusted.
 * @param[isPlayerTurn] Whether it is the turn of the player corresponding to this element.
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isPaused] Whether the clock is on pause.
 * @param[leaningSide] Which side the device is currently leaning towards.
 * @param[backEventAction] What action is executed by the back gesture.
 * @param[backEventProgress] Progress of the back gesture.
 * @param[backEventSwipeEdge] Swipe edge where the back gesture starts.
 * @param[isLandscape] Whether the device is in landscape mode.
 */
private fun GraphicsLayerScope.setBasicTimeGraphics(
    screenWidth: Dp,
    currentlyAdjustedAlpha: Float,
    isPlayerTurn: Boolean,
    isStarted: Boolean,
    isPaused: Boolean,
    leaningSide: LeaningSide,
    backEventAction: ClockBackAction,
    backEventProgress: Float,
    backEventSwipeEdge: Int,
    isLandscape: Boolean,
) {
    rotationZ = if (isLandscape) {
        0F
    } else when (leaningSide) {
        LeaningSide.LEFT -> 90F
        LeaningSide.RIGHT -> -90F
    }

    translationX = if (backEventAction == ClockBackAction.PAUSE && !isPlayerTurn) {
        0F
    } else {
        val sign = if (backEventSwipeEdge == BackEventCompat.EDGE_RIGHT) -1F else 1F
        sign * backEventProgress * screenWidth.toPx()
    }

    alpha = if (isPlayerTurn && isStarted && isPaused) {
        currentlyAdjustedAlpha
    } else {
        1F
    }
}
