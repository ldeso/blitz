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

import androidx.activity.BackEventCompat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.leodesouza.blitz.ui.components.LeaningSide
import net.leodesouza.blitz.ui.components.LeaningSideHandler
import net.leodesouza.blitz.ui.components.OrientationHandler
import net.leodesouza.blitz.ui.models.ClockState

/**
 * Minimalist Fischer chess clock.
 *
 * Default to 5+3 Fischer timing (5 minutes + 3 seconds per move). Total time and increment can be
 * set by horizontal and vertical dragging. The back action pauses or resets the clock.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriod] Period between ticks in milliseconds.
 * @param[dragSensitivity] How many minutes or seconds to add per dragged pixel.
 * @param[onClockStart] Callback called before the clock starts ticking.
 * @param[onClockPause] Callback called after the clock stops ticking.
 * @param[clockViewModel] ViewModel holding the state and logic for this screen.
 */
@Composable
fun ClockScreen(
    durationMinutes: Long = 5L,
    incrementSeconds: Long = 3L,
    tickPeriod: Long = 100L,
    dragSensitivity: Float = 0.01F,
    onClockStart: () -> Unit = {},
    onClockPause: () -> Unit = {},
    clockViewModel: ClockViewModel = viewModel {
        ClockViewModel(durationMinutes, incrementSeconds, tickPeriod)
    },
) {
    val whiteTime by clockViewModel.whiteTime.collectAsStateWithLifecycle()
    val blackTime by clockViewModel.blackTime.collectAsStateWithLifecycle()
    val clockState by clockViewModel.clockState.collectAsStateWithLifecycle()
    val playerState by clockViewModel.playerState.collectAsStateWithLifecycle()

    val displayOrientation = LocalConfiguration.current.orientation
    val layoutDirection = LocalLayoutDirection.current

    var orientation by remember { mutableIntStateOf(0) }
    var leaningSide by remember { mutableStateOf(LeaningSide.RIGHT) }
    var backEventProgress by remember { mutableFloatStateOf(0F) }
    var backEventSwipeEdge by remember {
        when (layoutDirection) {
            LayoutDirection.Ltr -> mutableIntStateOf(BackEventCompat.EDGE_LEFT)
            LayoutDirection.Rtl -> mutableIntStateOf(BackEventCompat.EDGE_RIGHT)
        }
    }
    var backEventAction by remember { mutableStateOf(ClockBackAction.PAUSE) }

    OrientationHandler(onOrientationChanged = { orientation = it })

    LeaningSideHandler(
        orientationProvider = { orientation },
        leaningSideProvider = { leaningSide },
        onLeaningSideChanged = {
            leaningSide = when (leaningSide) {
                LeaningSide.LEFT -> LeaningSide.RIGHT
                LeaningSide.RIGHT -> LeaningSide.LEFT
            }
        },
    )

    ClockStateHandler(
        whiteTimeProvider = { whiteTime },
        blackTimeProvider = { blackTime },
        clockStateProvider = { clockState },
        tick = clockViewModel::tick,
        onClockPause = onClockPause,
    )

    ClockBackHandler(
        clockStateProvider = { clockState },
        pause = clockViewModel::pause,
        resetTime = clockViewModel::resetTime,
        resetConf = clockViewModel::resetConf,
        saveTime = clockViewModel::saveTime,
        restoreSavedTime = { clockViewModel.restoreSavedTime(isDecimalRestored = true) },
        updateAction = { backEventAction = it },
        updateProgress = { backEventProgress = it },
        updateSwipeEdge = { backEventSwipeEdge = it },
    )

    Box(
        modifier = Modifier.clockInput(
            dragSensitivity = dragSensitivity,
            interactionSource = remember { MutableInteractionSource() },
            clockStateProvider = { clockState },
            leaningSideProvider = { leaningSide },
            displayOrientation = displayOrientation,
            layoutDirection = layoutDirection,
            start = {
                onClockStart()
                clockViewModel.start()
            },
            play = clockViewModel::play,
            saveTime = clockViewModel::saveTime,
            saveConf = clockViewModel::saveConf,
            restoreSavedTime = clockViewModel::restoreSavedTime,
            restoreSavedConf = clockViewModel::restoreSavedConf,
        ),
    ) {
        ClockContent(
            whiteTimeProvider = { whiteTime },
            blackTimeProvider = { blackTime },
            clockStateProvider = { clockState },
            playerStateProvider = { playerState },
            leaningSideProvider = { leaningSide },
            backEventActionProvider = { backEventAction },
            backEventProgressProvider = { backEventProgress },
            backEventSwipeEdgeProvider = { backEventSwipeEdge },
        )
    }
}

/**
 * Effect taking care of repeatedly waiting until the next tick when the clock is ticking, or
 * calling the callback [onClockPause] when the clock has stopped ticking.
 *
 * @param[whiteTimeProvider] Lambda for the time of the first player.
 * @param[blackTimeProvider] Lambda for the time of the second player.
 * @param[clockStateProvider] Lambda for the current state of the clock.
 * @param[tick] Callback called to wait until next tick.
 * @param[onClockPause] Callback called after the clock stops ticking.
 */
@Composable
private fun ClockStateHandler(
    whiteTimeProvider: () -> Long,
    blackTimeProvider: () -> Long,
    clockStateProvider: () -> ClockState,
    tick: suspend () -> Unit,
    onClockPause: () -> Unit,
) {
    val whiteTime = whiteTimeProvider()
    val blackTime = blackTimeProvider()
    val clockState = clockStateProvider()

    when (clockState) {
        ClockState.TICKING -> LaunchedEffect(whiteTime, blackTime) { tick() }
        else -> onClockPause()
    }
}
