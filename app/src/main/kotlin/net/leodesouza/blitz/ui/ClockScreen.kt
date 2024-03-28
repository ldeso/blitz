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
import net.leodesouza.blitz.ui.components.OrientationHandler

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
    val uiState by clockViewModel.uiState.collectAsStateWithLifecycle()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var orientation by remember { mutableIntStateOf(0) }
    var isLeaningRight by remember { mutableStateOf(true) }
    var backEventProgress by remember { mutableFloatStateOf(0F) }
    var backEventSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    OrientationHandler(onOrientationChanged = { orientation = it })

    IsLeaningRightHandler(
        orientationProvider = { orientation },
        isLeaningRightProvider = { isLeaningRight },
        onLeaningSideChanged = { isLeaningRight = !isLeaningRight },
    )

    ClockTickingEffect(
        whiteTimeProvider = { whiteTime },
        blackTimeProvider = { blackTime },
        isTickingProvider = { uiState.isTicking },
        isFinishedProvider = { uiState.isFinished },
        pause = {
            clockViewModel.pause()
            onClockPause()
        },
        tick = clockViewModel::tick,
    )

    ClockBackHandler(
        isStartedProvider = { uiState.isStarted },
        isTickingProvider = { uiState.isTicking },
        isDefaultConfProvider = { uiState.isDefaultConf },
        pause = {
            clockViewModel.pause()
            clockViewModel.restoreSavedTime(isDecimalRestored = true)
            onClockPause()
        },
        resetTime = clockViewModel::resetTime,
        resetConf = clockViewModel::resetConf,
        saveTime = clockViewModel::saveTime,
        updateProgress = { backEventProgress = it },
        updateSwipeEdge = { backEventSwipeEdge = it },
    )

    Box(
        modifier = Modifier.clockInput(
            dragSensitivity = dragSensitivity,
            interactionSource = remember { MutableInteractionSource() },
            isStartedProvider = { uiState.isStarted },
            isTickingProvider = { uiState.isTicking },
            isPausedProvider = { uiState.isPaused },
            isLeaningRightProvider = { isLeaningRight },
            isLandscape = isLandscape,
            isRtl = isRtl,
            start = {
                onClockStart()
                clockViewModel.start()
            },
            nextPlayer = clockViewModel::nextPlayer,
            saveTime = clockViewModel::saveTime,
            saveConf = clockViewModel::saveConf,
            restoreSavedTime = clockViewModel::restoreSavedTime,
            restoreSavedConf = clockViewModel::restoreSavedConf,
        ),
    ) {
        ClockContent(
            whiteTimeProvider = { whiteTime },
            blackTimeProvider = { blackTime },
            isWhiteTurnProvider = { uiState.isWhiteTurn },
            isStartedProvider = { uiState.isStarted },
            isTickingProvider = { uiState.isTicking },
            isPausedProvider = { uiState.isPaused },
            isLeaningRightProvider = { isLeaningRight },
            backEventProgressProvider = { backEventProgress },
            backEventSwipeEdgeProvider = { backEventSwipeEdge },
        )
    }
}

/**
 * Handle whether the device is currently leaning towards its right side.
 *
 * @param[orientationProvider] Lambda for the orientation of the device in degrees.
 * @param[isLeaningRightProvider] Lambda for whether the device is currently leaning right.
 * @param[onLeaningSideChanged] Callback called when the leaning side of the device changes.
 */
@Composable
private fun IsLeaningRightHandler(
    orientationProvider: () -> Int,
    isLeaningRightProvider: () -> Boolean,
    onLeaningSideChanged: () -> Unit,
) {
    val orientation = orientationProvider()
    val isLeaningRight = isLeaningRightProvider()

    val isChangingFromLeftToRight = !isLeaningRight && orientation in 10 until 170
    val isChangingFromRightToLeft = isLeaningRight && orientation in 190 until 350
    if (isChangingFromLeftToRight || isChangingFromRightToLeft) {
        onLeaningSideChanged()
    }
}

/**
 * Effect taking care of repeatedly waiting until the next tick or pausing the clock when it has
 * finished ticking.
 *
 * @param[whiteTimeProvider] Lambda for the time of the first player.
 * @param[blackTimeProvider] Lambda for the time of the second player.
 * @param[isTickingProvider] Lambda for whether the clock is currently ticking.
 * @param[isFinishedProvider] Lambda for whether the clock has finished ticking.
 * @param[pause] Callback called to pause the clock.
 * @param[tick] Callback called to wait until next tick.
 */
@Composable
private fun ClockTickingEffect(
    whiteTimeProvider: () -> Long,
    blackTimeProvider: () -> Long,
    isTickingProvider: () -> Boolean,
    isFinishedProvider: () -> Boolean,
    pause: () -> Unit,
    tick: suspend () -> Unit,
) {
    val whiteTime = whiteTimeProvider()
    val blackTime = blackTimeProvider()
    val isTicking = isTickingProvider()
    val isFinished = isFinishedProvider()

    if (isTicking) {
        if (isFinished) {
            pause()
        } else {
            LaunchedEffect(whiteTime, blackTime) { tick() }
        }
    }
}
