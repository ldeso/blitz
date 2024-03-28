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

import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * Effect making system back gestures pause or reset the clock.
 *
 * @param[isStartedProvider] Lambda for whether the clock has started ticking.
 * @param[isTickingProvider] Lambda for whether the clock is currently ticking.
 * @param[isDefaultConfProvider] Lambda for whether the clock is set to its default configuration.
 * @param[pause] Callback called to pause the clock.
 * @param[resetTime] Callback called to reset the time.
 * @param[resetConf] Callback called to reset the configuration.
 * @param[saveTime] Callback called to save the time.
 * @param[updateProgress] Callback called to update the progress of the back gesture.
 * @param[updateSwipeEdge] Callback called to update the swipe edge where the back gesture starts.
 */
@Composable
fun ClockBackHandler(
    isStartedProvider: () -> Boolean,
    isTickingProvider: () -> Boolean,
    isDefaultConfProvider: () -> Boolean,
    pause: () -> Unit,
    resetTime: () -> Unit,
    resetConf: () -> Unit,
    saveTime: () -> Unit,
    updateProgress: (Float) -> Unit,
    updateSwipeEdge: (Int) -> Unit,
) {
    val isStarted = isStartedProvider()
    val isTicking = isTickingProvider()
    val isDefaultConf = isDefaultConfProvider()

    PredictiveBackHandler(enabled = isTicking || isStarted || !isDefaultConf) { progress ->
        if (isTicking) saveTime()
        try {
            var backEventProgress = 0F
            progress.collect { backEvent ->
                backEventProgress = backEvent.progress
                updateProgress(backEventProgress)
                updateSwipeEdge(backEvent.swipeEdge)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                while (backEventProgress < 1F) {
                    delay(1L)
                    backEventProgress += 0.01F
                    updateProgress(backEventProgress)
                }
                delay(100L)
            }
            if (isTicking) {
                pause()
            } else if (isStarted) {
                resetTime()
            } else {
                resetConf()
            }
        } finally {
            updateProgress(0F)
        }
    }
}

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
fun Modifier.clockInput(
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
        onClockClickEvent(
            isTicking = isTickingProvider(),
            isPaused = isPausedProvider(),
            start = start,
            nextPlayer = nextPlayer,
        )
    }
        .onKeyEvent {
            onClockKeyEvent(
                keyEvent = it,
                isStarted = isStartedProvider(),
                isPaused = isPausedProvider(),
                isRtl = isRtl,
                saveTime = saveTime,
                saveConf = saveConf,
                restoreSavedTime = restoreSavedTime,
                restoreSavedConf = restoreSavedConf,
            )
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    onClockDragStart(
                        isStarted = isStartedProvider(),
                        isPaused = isPausedProvider(),
                        saveTime = saveTime,
                        saveConf = saveConf,
                    )
                },
                onDragEnd = {
                    onClockDragEnd(isTicking = isTickingProvider(), nextPlayer = nextPlayer)
                },
                onHorizontalDrag = { _: PointerInputChange, dragAmount: Float ->
                    onClockHorizontalDrag(
                        dragAmount = dragAmount,
                        dragSensitivity = dragSensitivity,
                        isStarted = isStartedProvider(),
                        isPaused = isPausedProvider(),
                        isLeaningRight = isLeaningRightProvider(),
                        isLandscape = isLandscape,
                        isRtl = isRtl,
                        restoreSavedTime = restoreSavedTime,
                        restoreSavedConf = restoreSavedConf,
                    )
                },
            )
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = {
                    onClockDragStart(
                        isStarted = isStartedProvider(),
                        isPaused = isPausedProvider(),
                        saveTime = saveTime,
                        saveConf = saveConf,
                    )
                },
                onDragEnd = {
                    onClockDragEnd(isTicking = isTickingProvider(), nextPlayer = nextPlayer)
                },
                onVerticalDrag = { _: PointerInputChange, dragAmount: Float ->
                    onClockVerticalDrag(
                        dragAmount = dragAmount,
                        dragSensitivity = dragSensitivity,
                        isStarted = isStartedProvider(),
                        isPaused = isPausedProvider(),
                        isLeaningRight = isLeaningRightProvider(),
                        isLandscape = isLandscape,
                        isRtl = isRtl,
                        restoreSavedTime = restoreSavedTime,
                        restoreSavedConf = restoreSavedConf,
                    )
                },
            )
        },
)

/**
 * Start the clock or switch to the next player on click events.
 *
 * @param[isTicking] Whether the clock is currently ticking.
 * @param[isPaused] Whether the clock is on pause.
 * @param[start] Callback called to start the clock.
 * @param[nextPlayer] Callback called to switch to the next player.
 */
private fun onClockClickEvent(
    isTicking: Boolean, isPaused: Boolean, start: () -> Unit, nextPlayer: () -> Unit,
) {
    if (isPaused) {
        start()
    } else if (isTicking) {
        nextPlayer()
    }
}

/**
 * Change the time or the configuration of the clock on key presses.
 *
 * @param[keyEvent] The key event to intercept.
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isPaused] Whether the clock is on pause.
 * @param[isRtl] Whether the layout direction is configured from right to left.
 * @param[saveTime] Callback called to save the time.
 * @param[saveConf] Callback called to save the configuration.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[restoreSavedConf] Callback called to restore the saved configuration.
 */
private fun onClockKeyEvent(
    keyEvent: KeyEvent,
    isStarted: Boolean,
    isPaused: Boolean,
    isRtl: Boolean,
    saveTime: () -> Unit,
    saveConf: () -> Unit,
    restoreSavedTime: (Float, Float) -> Unit,
    restoreSavedConf: (Float, Float) -> Unit,
): Boolean {
    if (keyEvent.type == KeyEventType.KeyDown && isPaused) {
        var addMinutes = 0F
        var addSeconds = 0F

        when (keyEvent.key) {
            Key.DirectionUp -> addSeconds = 1F
            Key.DirectionDown -> addSeconds = -1F
            Key.DirectionRight -> addMinutes = if (isRtl) -1F else 1F
            Key.DirectionLeft -> addMinutes = if (isRtl) 1F else -1F
            else -> return false
        }

        if (isStarted) {
            saveTime()
            restoreSavedTime(addMinutes, addSeconds)
        } else {
            saveConf()
            restoreSavedConf(addMinutes, addSeconds)
        }
        return true
    }
    return false
}

/**
 * Save the current time/configuration if the clock is on pause at the beginning of a drag gesture.
 *
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isPaused] Whether the clock is on pause.
 * @param[saveTime] Callback called to save the time.
 * @param[saveConf] Callback called to save the configuration.
 */
private fun onClockDragStart(
    isStarted: Boolean, isPaused: Boolean, saveTime: () -> Unit, saveConf: () -> Unit,
) {
    if (isPaused) {
        if (isStarted) {
            saveTime()
        } else {
            saveConf()
        }
    }
}

/**
 * Switch to the next player if the clock is ticking at the end of a drag gesture.
 *
 * @param[isTicking] Whether the clock is currently ticking.
 * @param[nextPlayer] Callback called to switch to the next player.
 */
private fun onClockDragEnd(isTicking: Boolean, nextPlayer: () -> Unit) {
    if (isTicking) {
        nextPlayer()
    }
}

/**
 * Add seconds (in portrait orientation) or minutes (in landscape orientation) to the current time
 * or configuration of the clock during drag events.
 *
 * @param[dragAmount] How many pixels are dragged during the drag gesture.
 * @param[dragSensitivity] How many minutes or seconds to add per dragged pixel.
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isPaused] Whether the clock is on pause.
 * @param[isLeaningRight] Whether the device is leaning right.
 * @param[isLandscape] Whether the device is in landscape mode.
 * @param[isRtl] Whether the layout direction is configured from right to left.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[restoreSavedConf] Callback called to restore the saved configuration.
 */
private fun onClockHorizontalDrag(
    dragAmount: Float,
    dragSensitivity: Float,
    isStarted: Boolean,
    isPaused: Boolean,
    isLeaningRight: Boolean,
    isLandscape: Boolean,
    isRtl: Boolean,
    restoreSavedTime: (Float, Float) -> Unit,
    restoreSavedConf: (Float, Float) -> Unit,
) {
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
}

/**
 * Add minutes (in portrait orientation) or seconds (in landscape orientation) to the current time
 * or configuration of the clock during drag events.
 *
 * @param[dragAmount] How many pixels are dragged during the drag gesture.
 * @param[dragSensitivity] How many minutes or seconds to add per dragged pixel.
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isPaused] Whether the clock is on pause.
 * @param[isLeaningRight] Whether the device is leaning right.
 * @param[isLandscape] Whether the device is in landscape mode.
 * @param[isRtl] Whether the layout direction is configured from right to left.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[restoreSavedConf] Callback called to restore the saved configuration.
 */
private fun onClockVerticalDrag(
    dragAmount: Float,
    dragSensitivity: Float,
    isStarted: Boolean,
    isPaused: Boolean,
    isLeaningRight: Boolean,
    isLandscape: Boolean,
    isRtl: Boolean,
    restoreSavedTime: (Float, Float) -> Unit,
    restoreSavedConf: (Float, Float) -> Unit,
) {
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
}
