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

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * A minimalist Fischer chess clock for Android.
 *
 * Default to 5+3 Fischer timing (5 minutes + 3 seconds per move). Total time and increment can be
 * set by horizontal and vertical dragging. The back action pauses or resets the clock.
 *
 * @param[isLeaningRight] Whether the dragging direction is reversed in portrait mode.
 * @param[onStart] Callback called when the clock starts ticking.
 * @param[onPause] Callback called when the clock stops ticking.
 * @param[clock] ViewModel holding the state and logic for this screen.
 */
@Composable
fun ChessClockScreen(
    isLeaningRight: () -> Boolean,
    onStart: () -> Unit = {},
    onPause: () -> Unit = {},
    clock: ChessClockViewModel = viewModel {
        ChessClockViewModel(
            durationMinutes = 5L,
            incrementSeconds = 3L,
            tickPeriod = 100L,
            onStart = onStart,
            onPause = onPause,
        )
    },
) {
    val uiState by clock.uiState.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dragSensitivity = 0.01F

    BackHandler(uiState.isTicking) { clock.pause() }
    BackHandler(uiState.isStarted && !uiState.isTicking) { clock.resetTime() }
    BackHandler(!uiState.isStarted && !uiState.isDefaultConfig) { clock.resetConfig() }

    LaunchedEffect(uiState.isTicking, uiState.whiteTime, uiState.blackTime) {
        if (uiState.isTicking) {
            if (uiState.isFinished) clock.pause() else clock.tick()
        }
    }

    Box(modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                if (uiState.isPaused) {
                    clock.start()
                } else if (uiState.isTicking) {
                    clock.nextPlayer()
                }
            },
        )
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { if (uiState.isPaused) clock.saveTime() },
                onDragEnd = { if (uiState.isTicking) clock.nextPlayer() },
                onHorizontalDrag = { _: PointerInputChange, dragAmount: Float ->
                    if (uiState.isPaused) {
                        if (isLandscape) {
                            val sign = if (isRtl) -1F else 1F
                            val minutes = sign * dragSensitivity * dragAmount
                            clock.addMinutes(minutes, isAddedToSavedTime = true)
                        } else {
                            val sign = if (isLeaningRight.invoke()) -1F else 1F
                            val seconds = sign * dragSensitivity * dragAmount
                            clock.addSeconds(seconds, isAddedToSavedTime = true)
                        }
                    }
                },
            )
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { if (uiState.isPaused) clock.saveTime() },
                onDragEnd = { if (uiState.isTicking) clock.nextPlayer() },
                onVerticalDrag = { _: PointerInputChange, dragAmount: Float ->
                    if (uiState.isPaused) {
                        if (isLandscape) {
                            val sign = -1F
                            val seconds = sign * dragSensitivity * dragAmount
                            clock.addSeconds(seconds, isAddedToSavedTime = true)
                        } else {
                            val sign = if (isLeaningRight.invoke() xor isRtl) -1F else 1F
                            val minutes = sign * dragSensitivity * dragAmount
                            clock.addMinutes(minutes, isAddedToSavedTime = true)
                        }
                    }
                },
            )
        }
        .onKeyEvent(onKeyEvent = {
            var isConsumed = false
            if (it.type == KeyEventType.KeyDown) {
                isConsumed = true
                if (uiState.isPaused) {
                    when (it.key) {
                        Key.DirectionUp -> clock.addSeconds(1F)
                        Key.DirectionDown -> clock.addSeconds(-1F)
                        Key.DirectionRight -> clock.addMinutes(if (isRtl) -1F else 1F)
                        Key.DirectionLeft -> clock.addMinutes(if (isRtl) 1F else -1F)
                        else -> isConsumed = false
                    }
                }
            }
            isConsumed
        })
    ) {
        ChessClockContent(uiState.whiteTime, uiState.blackTime, isLeaningRight.invoke())
    }
}

/**
 * Chess clock screen content displaying the remaining [whiteTime] and [blackTime], and where in
 * portrait configurations the text is rotated by ninety degrees in a direction that depends on
 * whether the device [isLeaningRight].
 */
@Preview
@Composable
fun ChessClockContent(
    whiteTime: Long = 303_000L, blackTime: Long = 303_000L, isLeaningRight: Boolean = true
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val rotation = if (isLandscape) {
        0F
    } else {
        if (isLeaningRight) -90F else 90F
    }
    val textHeight = LocalConfiguration.current.screenHeightDp.dp / if (isLandscape) 3 else 8
    val fontSize = with(LocalDensity.current) { textHeight.toSp() }

    Column {
        BasicTime(
            blackTime,
            modifier = Modifier
                .background(Color.Black)
                .rotate(rotation)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(
                color = if (blackTime > 0L) Color.White else Color.Red,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            ),
        )
        BasicTime(
            whiteTime,
            modifier = Modifier
                .background(Color.White)
                .rotate(rotation)
                .weight(1F)
                .fillMaxSize()
                .wrapContentSize(),
            style = TextStyle(
                color = if (whiteTime > 0L) Color.Black else Color.Red,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            ),
        )
    }
}

/**
 * Basic element that displays a [timeMillis] in the format "MM:SS.D" or "H:MM:SS.D", in a given
 * [style] and accepting a given [modifier] to apply to its layout node.
 */
@Composable
fun BasicTime(
    timeMillis: Long, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default
) {
    val timeTenthsOfSeconds = (timeMillis + 99L) / 100L  // round up to the nearest tenth of second
    val hours = timeTenthsOfSeconds / 36_000L
    val minutes = timeTenthsOfSeconds % 36_000L / 600L
    val seconds = timeTenthsOfSeconds % 600L / 10L
    val tenthsOfSeconds = timeTenthsOfSeconds % 10L
    val monospace = style.merge(fontFamily = FontFamily.Monospace)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier) {
            if (hours != 0L) {
                BasicText("$hours", style = monospace)
                BasicText(":", style = style)
            }
            BasicText("$minutes".padStart(2, '0'), style = monospace)
            BasicText(":", style = style)
            BasicText("$seconds".padStart(2, '0'), style = monospace)
            if (hours == 0L) {
                BasicText(".", style = style)
                BasicText("$tenthsOfSeconds", style = monospace)
            }
        }
    }
}
