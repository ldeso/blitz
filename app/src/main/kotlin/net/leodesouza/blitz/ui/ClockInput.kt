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
import android.content.res.Configuration.ORIENTATION_PORTRAIT
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
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import net.leodesouza.blitz.ui.components.LeaningSide
import net.leodesouza.blitz.ui.models.ClockState

/** What action is executed by a back gesture. */
enum class ClockBackAction { PAUSE, RESET_TIME, RESET_CONF }

/**
 * Effect making system back gestures pause or reset the clock.
 *
 * @param[clockStateProvider] Lambda for the current state of the clock.
 * @param[pause] Callback called to pause the clock.
 * @param[resetTime] Callback called to reset the time.
 * @param[resetConf] Callback called to reset the configuration.
 * @param[saveTime] Callback called to save the time.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[updateProgress] Callback called to update the progress of the back gesture.
 * @param[updateSwipeEdge] Callback called to update the swipe edge where the back gesture starts.
 */
@Composable
fun ClockBackHandler(
    clockStateProvider: () -> ClockState,
    pause: () -> Unit,
    resetTime: () -> Unit,
    resetConf: () -> Unit,
    saveTime: () -> Unit,
    restoreSavedTime: () -> Unit,
    updateAction: (ClockBackAction) -> Unit,
    updateProgress: (Float) -> Unit,
    updateSwipeEdge: (Int) -> Unit,
) {
    val clockState = clockStateProvider()

    PredictiveBackHandler(enabled = clockState != ClockState.FULL_RESET) { backEvent ->
        // beginning of back gesture
        val action = when (clockState) {
            ClockState.TICKING -> ClockBackAction.PAUSE
            ClockState.PAUSED, ClockState.FINISHED -> ClockBackAction.RESET_TIME
            else -> ClockBackAction.RESET_CONF
        }
        updateAction(action)
        if (action == ClockBackAction.PAUSE) saveTime()

        try {
            var progress = 0F
            backEvent.collect {  // during progressive back gesture
                progress = it.progress
                updateProgress(progress)
                updateSwipeEdge(it.swipeEdge)
            }

            // completion of back gesture
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                while (progress < 1F) {
                    delay(1L)
                    progress += 0.01F
                    updateProgress(progress)
                }
                delay(100L)
            }
            when (action) {
                ClockBackAction.PAUSE -> pause()
                ClockBackAction.RESET_TIME -> resetTime()
                ClockBackAction.RESET_CONF -> resetConf()
            }
            if (action == ClockBackAction.PAUSE) restoreSavedTime()

        } finally {
            updateProgress(0F)  // after back gesture
        }
    }
}

/**
 * Modifier to control the chess clock through click events, dragging events and key presses.
 *
 * @param[dragSensitivity] How many minutes or seconds to add per dragged pixel.
 * @param[interactionSource] Mutable interaction source used to dispatch click events.
 * @param[clockStateProvider] Lambda for the current state of the clock.
 * @param[leaningSideProvider] Lambda for which side the device is currently leaning towards.
 * @param[displayOrientation] The [ORIENTATION_PORTRAIT] or [ORIENTATION_LANDSCAPE] of the display.
 * @param[layoutDirection] Whether the layout direction is left-to-right or right-to-left.
 * @param[start] Callback called to start the clock.
 * @param[play] Callback called to switch to the next player.
 * @param[saveTime] Callback called to save the time.
 * @param[saveConf] Callback called to save the configuration.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[restoreSavedConf] Callback called to restore the saved configuration.
 */
fun Modifier.clockInput(
    dragSensitivity: Float,
    interactionSource: MutableInteractionSource,
    clockStateProvider: () -> ClockState,
    leaningSideProvider: () -> LeaningSide,
    displayOrientation: Int,  // ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
    layoutDirection: LayoutDirection,
    start: () -> Unit,
    play: () -> Unit,
    saveTime: () -> Unit,
    saveConf: () -> Unit,
    restoreSavedTime: (addMinutes: Float, addSeconds: Float) -> Unit,
    restoreSavedConf: (addMinutes: Float, addSeconds: Float) -> Unit,
): Modifier = then(
    clickable(interactionSource = interactionSource, indication = null) {
        onClickEvent(clockState = clockStateProvider(), start = start, play = play)
    }
        .onKeyEvent {
            onKeyEvent(
                keyEvent = it,
                clockState = clockStateProvider(),
                layoutDirection = layoutDirection,
                changeTime = { addMinutes, addSeconds ->
                    saveTime()
                    restoreSavedTime(addMinutes, addSeconds)
                },
                changeConf = { addMinutes, addSeconds ->
                    saveConf()
                    restoreSavedConf(addMinutes, addSeconds)
                },
            )
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    onDragStart(
                        clockState = clockStateProvider(), saveTime = saveTime, saveConf = saveConf,
                    )
                },
                onDragEnd = { onDragEnd(clockState = clockStateProvider(), play = play) },
                onHorizontalDrag = { _: PointerInputChange, dragAmount: Float ->
                    onHorizontalDrag(
                        addAmount = dragSensitivity * dragAmount,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        displayOrientation = displayOrientation,
                        layoutDirection = layoutDirection,
                        restoreSavedTime = restoreSavedTime,
                        restoreSavedConf = restoreSavedConf,
                    )
                },
            )
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = {
                    onDragStart(
                        clockState = clockStateProvider(), saveTime = saveTime, saveConf = saveConf,
                    )
                },
                onDragEnd = { onDragEnd(clockState = clockStateProvider(), play = play) },
                onVerticalDrag = { _: PointerInputChange, dragAmount: Float ->
                    onVerticalDrag(
                        addAmount = dragSensitivity * dragAmount,
                        clockState = clockStateProvider(),
                        leaningSide = leaningSideProvider(),
                        displayOrientation = displayOrientation,
                        layoutDirection = layoutDirection,
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
 * @param[clockState] Current state of the clock.
 * @param[start] Callback called to start the clock.
 * @param[play] Callback called to switch to the next player.
 */
private fun onClickEvent(clockState: ClockState, start: () -> Unit, play: () -> Unit) {
    when (clockState) {
        ClockState.PAUSED, ClockState.SOFT_RESET, ClockState.FULL_RESET -> start()
        ClockState.TICKING -> play()
        else -> Unit
    }
}

/**
 * Change the time or the configuration of the clock on key presses.
 *
 * @param[keyEvent] The key event to intercept.
 * @param[clockState] Current state of the clock.
 * @param[layoutDirection] Whether the layout direction is left-to-right or right-to-left.
 * @param[changeTime] Callback called to change the time.
 * @param[changeConf] Callback called to change the configuration.
 */
private fun onKeyEvent(
    keyEvent: KeyEvent,
    clockState: ClockState,
    layoutDirection: LayoutDirection,
    changeTime: (addMinutes: Float, addSeconds: Float) -> Unit,
    changeConf: (addMinutes: Float, addSeconds: Float) -> Unit,
): Boolean {
    if (keyEvent.type == KeyEventType.KeyDown) {
        when (clockState) {
            ClockState.PAUSED, ClockState.SOFT_RESET, ClockState.FULL_RESET -> run {
                var addMinutes = 0F
                var addSeconds = 0F
                val isLtr = when (layoutDirection) {
                    LayoutDirection.Ltr -> true
                    LayoutDirection.Rtl -> false
                }

                when (keyEvent.key) {
                    Key.DirectionUp -> addSeconds = 1F
                    Key.DirectionDown -> addSeconds = -1F
                    Key.DirectionRight -> addMinutes = if (isLtr) 1F else -1F
                    Key.DirectionLeft -> addMinutes = if (isLtr) -1F else 1F
                    else -> return false
                }

                when (clockState) {
                    ClockState.PAUSED -> changeTime(addMinutes, addSeconds)
                    else -> changeConf(addMinutes, addSeconds)
                }

                return true
            }

            else -> return false
        }
    } else {
        return false
    }
}

/**
 * Save the current time/configuration if the clock is on pause at the beginning of a drag gesture.
 *
 * @param[clockState] Current state of the clock.
 * @param[saveTime] Callback called to save the time.
 * @param[saveConf] Callback called to save the configuration.
 */
private fun onDragStart(clockState: ClockState, saveTime: () -> Unit, saveConf: () -> Unit) {
    when (clockState) {
        ClockState.PAUSED -> saveTime()
        ClockState.SOFT_RESET, ClockState.FULL_RESET -> saveConf()
        else -> Unit
    }
}

/**
 * Switch to the next player if the clock is ticking at the end of a drag gesture.
 *
 * @param[clockState] Current state of the clock.
 * @param[play] Callback called to switch to the next player.
 */
private fun onDragEnd(clockState: ClockState, play: () -> Unit) {
    if (clockState == ClockState.TICKING) {
        play()
    }
}

/**
 * Add seconds (in portrait orientation) or minutes (in landscape orientation) to the current time
 * or configuration of the clock during drag events.
 *
 * @param[addAmount] How many minutes or seconds to add.
 * @param[clockState] Current state of the clock.
 * @param[leaningSide] Which side the device is currently leaning towards.
 * @param[displayOrientation] The [ORIENTATION_PORTRAIT] or [ORIENTATION_LANDSCAPE] of the display.
 * @param[layoutDirection] Whether the layout direction is left-to-right or right-to-left.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[restoreSavedConf] Callback called to restore the saved configuration.
 */
private fun onHorizontalDrag(
    addAmount: Float,
    clockState: ClockState,
    leaningSide: LeaningSide,
    displayOrientation: Int,  // ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
    layoutDirection: LayoutDirection,
    restoreSavedTime: (Float, Float) -> Unit,
    restoreSavedConf: (Float, Float) -> Unit,
) {
    when (clockState) {
        ClockState.PAUSED, ClockState.SOFT_RESET, ClockState.FULL_RESET -> run {
            var addMinutes = 0F
            var addSeconds = 0F

            when (displayOrientation) {
                ORIENTATION_PORTRAIT -> addSeconds = when (leaningSide) {
                    LeaningSide.LEFT -> addAmount
                    LeaningSide.RIGHT -> -addAmount
                }

                ORIENTATION_LANDSCAPE -> addMinutes = when (layoutDirection) {
                    LayoutDirection.Ltr -> addAmount
                    LayoutDirection.Rtl -> -addAmount
                }
            }

            when (clockState) {
                ClockState.PAUSED -> restoreSavedTime(addMinutes, addSeconds)
                else -> restoreSavedConf(addMinutes, addSeconds)
            }
        }

        else -> Unit
    }
}

/**
 * Add minutes (in portrait orientation) or seconds (in landscape orientation) to the current time
 * or configuration of the clock during drag events.
 *
 * @param[addAmount] How many minutes or seconds to add.
 * @param[clockState] Current state of the clock.
 * @param[leaningSide] Which side the device is currently leaning towards.
 * @param[displayOrientation] The [ORIENTATION_PORTRAIT] or [ORIENTATION_LANDSCAPE] of the display.
 * @param[layoutDirection] Whether the layout direction is left-to-right or right-to-left.
 * @param[restoreSavedTime] Callback called to restore the saved time.
 * @param[restoreSavedConf] Callback called to restore the saved configuration.
 */
private fun onVerticalDrag(
    addAmount: Float,
    clockState: ClockState,
    leaningSide: LeaningSide,
    displayOrientation: Int,  // ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
    layoutDirection: LayoutDirection,
    restoreSavedTime: (Float, Float) -> Unit,
    restoreSavedConf: (Float, Float) -> Unit,
) {
    when (clockState) {
        ClockState.PAUSED, ClockState.SOFT_RESET, ClockState.FULL_RESET -> run {
            var addMinutes = 0F
            var addSeconds = 0F
            val isLtr = when (layoutDirection) {
                LayoutDirection.Ltr -> true
                LayoutDirection.Rtl -> false
            }

            when (displayOrientation) {
                ORIENTATION_PORTRAIT -> addMinutes = when (leaningSide) {
                    LeaningSide.LEFT -> if (isLtr) addAmount else -addAmount
                    LeaningSide.RIGHT -> if (isLtr) -addAmount else addAmount
                }

                ORIENTATION_LANDSCAPE -> addSeconds = -addAmount
            }

            when (clockState) {
                ClockState.PAUSED -> restoreSavedTime(addMinutes, addSeconds)
                else -> restoreSavedConf(addMinutes, addSeconds)
            }
        }

        else -> Unit
    }
}
