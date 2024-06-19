// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import android.content.Context
import android.media.AudioManager
import androidx.activity.BackEventCompat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
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
 * Defaults to 5+3 Fischer timing (5 minutes + 3 seconds per move). Touching anywhere on the screen
 * switches to the next player. Time and increment are set by horizontal and vertical dragging.
 * Haptic feedback is enabled by setting the ringtone to vibrate. Back gestures pause and reset the
 * clock.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriodMillis] Period between ticks in milliseconds.
 * @param[dragSensitivity] How many minutes or seconds to add per dragged pixel.
 * @param[onClockStart] Callback called before the clock starts ticking.
 * @param[onClockStop] Callback called after the clock stops ticking.
 * @param[clockViewModel] ViewModel holding the state and logic for this screen.
 */
@Composable
fun ClockScreen(
    durationMinutes: Int = 5,
    incrementSeconds: Int = 3,
    tickPeriodMillis: Int = 100,
    dragSensitivity: Float = 0.01F,
    onClockStart: () -> Unit = {},
    onClockStop: () -> Unit = {},
    clockViewModel: ClockViewModel = viewModel {
        ClockViewModel(durationMinutes, incrementSeconds, tickPeriodMillis)
    },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val displayOrientation = LocalConfiguration.current.orientation
    val layoutDirection = LocalLayoutDirection.current
    val audioManager = LocalContext.current.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val haptics = LocalHapticFeedback.current

    val whiteTime by clockViewModel.whiteTime.collectAsStateWithLifecycle(lifecycleOwner)
    val blackTime by clockViewModel.blackTime.collectAsStateWithLifecycle(lifecycleOwner)
    val clockState by clockViewModel.clockState.collectAsStateWithLifecycle(lifecycleOwner)
    val playerState by clockViewModel.playerState.collectAsStateWithLifecycle(lifecycleOwner)

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
    val isBusy by remember { derivedStateOf { backEventProgress != 0F } }

    CallbackHandler(
        clockStateProvider = { clockState },
        onClockStart = onClockStart,
        onClockStop = onClockStop,
    )

    OrientationHandler { orientation = it }

    LeaningSideHandler(
        orientationProvider = { orientation }, leaningSideProvider = { leaningSide },
    ) {
        leaningSide = when (leaningSide) {
            LeaningSide.LEFT -> LeaningSide.RIGHT
            LeaningSide.RIGHT -> LeaningSide.LEFT
        }
    }

    ClockBackHandler(
        clockStateProvider = { clockState },
        isBusyProvider = { isBusy },
        pause = clockViewModel::pause,
        reset = clockViewModel::reset,
        save = clockViewModel::save,
        restore = { clockViewModel.restore(isDecimalRestored = true) },
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
            isBusyProvider = { isBusy },
            displayOrientation = displayOrientation,
            layoutDirection = layoutDirection,
            audioManager = audioManager,
            haptics = haptics,
            start = {
                onClockStart()
                clockViewModel.start()
            },
            play = clockViewModel::play,
            save = clockViewModel::save,
            restore = clockViewModel::restore,
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
 * Effect taking care of calling the callbacks [onClockStart] and [onClockStop] depending on the
 * state of the clock returned by [clockStateProvider].
 */
@Composable
private fun CallbackHandler(
    clockStateProvider: () -> ClockState,
    onClockStart: () -> Unit,
    onClockStop: () -> Unit,
) {
    val clockState = clockStateProvider()

    when (clockState) {
        ClockState.TICKING -> onClockStart()
        ClockState.PAUSED, ClockState.FINISHED -> onClockStop()
        else -> Unit
    }
}
