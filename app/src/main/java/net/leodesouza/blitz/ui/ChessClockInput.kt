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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier to control the chess clock through click events, dragging events and key presses.
 *
 * @param[dragSensitivity] How many minutes or seconds to add per dragged pixel.
 * @param[interactionSource] Mutable interaction source used to dispatch click events.
 * @param[isStartedProvider] Lambda for whether the clock has started ticking.
 * @param[isTickingProvider] Lambda for whether the clock is currently ticking.
 * @param[isPausedProvider] Lambda for whether the clock is on pause.
 * @param[isLeaningRightProvider] Lambda for whether the device is leaning right.
 * @param[isLandscape] Whether the device is in landscape mode.
 * @param[isRtl] Whether the layout direction is configured from right to left.
 * @param[start] Callback called to start the clock.
 * @param[nextPlayer] Callback called to switch to the next player.
 * @param[saveTime] Callback called to save the time.
 * @param[saveConf] Callback called to save the configuration.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[restoreSavedConf] Callback called to restore the saved configuration.
 */
fun Modifier.chessClockInput(
    dragSensitivity: Float,
    interactionSource: MutableInteractionSource,
    isStartedProvider: () -> Boolean,
    isTickingProvider: () -> Boolean,
    isPausedProvider: () -> Boolean,
    isLeaningRightProvider: () -> Boolean,
    isLandscape: Boolean,
    isRtl: Boolean,
    start: () -> Unit,
    nextPlayer: () -> Unit,
    saveTime: () -> Unit,
    saveConf: () -> Unit,
    restoreSavedTime: (Float, Float) -> Unit,
    restoreSavedConf: (Float, Float) -> Unit,
): Modifier = then(
    clickable(interactionSource = interactionSource, indication = null) {
        val isTicking = isTickingProvider()
        val isPaused = isPausedProvider()

        if (isPaused) {
            start()
        } else if (isTicking) {
            nextPlayer()
        }
    }

        .onKeyEvent {
            val isStarted = isStartedProvider()
            val isPaused = isPausedProvider()

            if (it.type == KeyEventType.KeyDown && isPaused) {
                var addMinutes = 0F
                var addSeconds = 0F

                when (it.key) {
                    Key.DirectionUp -> addSeconds = 1F
                    Key.DirectionDown -> addSeconds = -1F
                    Key.DirectionRight -> addMinutes = if (isRtl) -1F else 1F
                    Key.DirectionLeft -> addMinutes = if (isRtl) 1F else -1F
                    else -> return@onKeyEvent false
                }

                if (isStarted) {
                    saveTime()
                    restoreSavedTime(addMinutes, addSeconds)
                } else {
                    saveConf()
                    restoreSavedConf(addMinutes, addSeconds)
                }
                return@onKeyEvent true
            }
            false
        }

        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    val isStarted = isStartedProvider()
                    val isPaused = isPausedProvider()

                    if (isPaused) {
                        if (isStarted) {
                            saveTime()
                        } else {
                            saveConf()
                        }
                    }
                },

                onDragEnd = {
                    val isTicking = isTickingProvider()

                    if (isTicking) {
                        nextPlayer()
                    }
                },

                onHorizontalDrag = { _: PointerInputChange, dragAmount: Float ->
                    val isStarted = isStartedProvider()
                    val isPaused = isPausedProvider()
                    val isLeaningRight = isLeaningRightProvider()

                    if (isPaused) {
                        if (isLandscape) {
                            val sign = if (isRtl) -1F else 1F
                            val addMinutes = sign * dragSensitivity * dragAmount
                            val addSeconds = 0F
                            if (isStarted) {
                                restoreSavedTime(addMinutes, addSeconds)
                            } else {
                                restoreSavedConf(addMinutes, addSeconds)
                            }
                        } else {
                            val sign = if (isLeaningRight) -1F else 1F
                            val addMinutes = 0F
                            val addSeconds = sign * dragSensitivity * dragAmount
                            if (isStarted) {
                                restoreSavedTime(addMinutes, addSeconds)
                            } else {
                                restoreSavedConf(addMinutes, addSeconds)
                            }
                        }
                    }
                },
            )
        }

        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = {
                    val isStarted = isStartedProvider()
                    val isPaused = isPausedProvider()

                    if (isPaused) {
                        if (isStarted) {
                            saveTime()
                        } else {
                            saveConf()
                        }
                    }
                },

                onDragEnd = {
                    val isTicking = isTickingProvider()

                    if (isTicking) {
                        nextPlayer()
                    }
                },

                onVerticalDrag = { _: PointerInputChange, dragAmount: Float ->
                    val isStarted = isStartedProvider()
                    val isPaused = isPausedProvider()
                    val isLeaningRight = isLeaningRightProvider()

                    if (isPaused) {
                        if (isLandscape) {
                            val sign = -1F
                            val addMinutes = 0F
                            val addSeconds = sign * dragSensitivity * dragAmount
                            if (isStarted) {
                                restoreSavedTime(addMinutes, addSeconds)
                            } else {
                                restoreSavedConf(addMinutes, addSeconds)
                            }
                        } else {
                            val sign = if (isLeaningRight xor isRtl) -1F else 1F
                            val addMinutes = sign * dragSensitivity * dragAmount
                            val addSeconds = 0F
                            if (isStarted) {
                                restoreSavedTime(addMinutes, addSeconds)
                            } else {
                                restoreSavedConf(addMinutes, addSeconds)
                            }
                        }
                    }
                },
            )
        },
)
