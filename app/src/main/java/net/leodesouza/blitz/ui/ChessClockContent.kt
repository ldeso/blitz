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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.leodesouza.blitz.ui.components.BasicTime
import kotlin.math.roundToInt

/**
 * Minimalist content for the chess clock screen.
 *
 * @param[whiteTimeProvider] Lambda for the remaining time for the first player.
 * @param[blackTimeProvider] Lambda for the remaining time for the second player.
 * @param[isWhiteTurnProvider] Lambda for whether it is the turn of the first or the second player.
 * @param[isTickingProvider] Lambda for whether the clock is currently ticking.
 * @param[isLeaningRightProvider] Lambda for whether the device is leaning right.
 * @param[backProgressProvider] Lambda for the progress of a progressive back event.
 * @param[backSwipeEdgeProvider] Lambda for the swipe edge of a back event.
 */
@Composable
fun ChessClockContent(
    whiteTimeProvider: () -> Long,
    blackTimeProvider: () -> Long,
    isWhiteTurnProvider: () -> Boolean,
    isTickingProvider: () -> Boolean,
    isLeaningRightProvider: () -> Boolean,
    backProgressProvider: () -> Float,
    backSwipeEdgeProvider: () -> Int,
) {
    val isLeaningRight = isLeaningRightProvider()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val rotation = if (isLandscape) {
        0F
    } else if (isLeaningRight) {
        -90F
    } else {
        90F
    }
    val timeOverColor = Color.Red //.copy(alpha = alpha)
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val textHeight = screenHeight / if (isLandscape) 3 else 8
    val fontSize = with(density) { textHeight.toSp() }
    val fontWeight = FontWeight.Bold
    val swipeSpeed = 3F

    Column {
        val reusableItemModifier = Modifier
            .rotate(rotation)
            .weight(1F)
            .fillMaxSize()
            .wrapContentSize()
        BasicTime(
            timeProvider = blackTimeProvider,
            modifier = Modifier
                .background(Color.Black)
                .absoluteOffset {
                    IntOffset(
                        x = if (!isTickingProvider() || !isWhiteTurnProvider()) {
                            val backProgress = backProgressProvider()
                            val backSwipeEdge = backSwipeEdgeProvider()
                            val sign = if (backSwipeEdge == BackEventCompat.EDGE_RIGHT) -1 else 1
                            sign * (backProgress * swipeSpeed * screenWidth.toPx()).roundToInt()
                        } else {
                            0
                        },
                        y = 0,
                    )
                }
                .then(reusableItemModifier),
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = Color.White //.copy(alpha = alpha),
            ),
        )
        BasicTime(
            timeProvider = whiteTimeProvider,
            modifier = Modifier
                .background(Color.White)
                .absoluteOffset {
                    IntOffset(
                        x = if (!isTickingProvider() || isWhiteTurnProvider()) {
                            val backProgress = backProgressProvider()
                            val backSwipeEdge = backSwipeEdgeProvider()
                            val sign = if (backSwipeEdge == BackEventCompat.EDGE_RIGHT) -1 else 1
                            sign * (backProgress * swipeSpeed * screenWidth.toPx()).roundToInt()
                        } else {
                            0
                        },
                        y = 0,
                    )
                }
                .then(reusableItemModifier),
            style = TextStyle(fontSize = fontSize, fontWeight = fontWeight, color = Color.Black),
        )
    }
}

@Preview
@Composable
fun ChessClockContentPreview() {
    ChessClockContent(
        whiteTimeProvider = { 303_000L },
        blackTimeProvider = { 303_000L },
        isWhiteTurnProvider = { true },
        isTickingProvider = { false },
        isLeaningRightProvider = { true },
        backProgressProvider = { 0F },
        backSwipeEdgeProvider = { BackEventCompat.EDGE_RIGHT },
    )
}