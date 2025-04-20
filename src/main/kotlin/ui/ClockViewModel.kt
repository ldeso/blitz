// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * ViewModel holding state and logic for the chess clock screen.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriodMillis] Period between ticks in milliseconds.
 */
class ClockViewModel(
    durationMinutes: Int,
    incrementSeconds: Int,
    tickPeriodMillis: Int,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) : ViewModel() {
    private val defaultDuration: Duration = durationMinutes.minutes
    private val defaultIncrement: Duration = incrementSeconds.seconds
    private val tickPeriodMillis: Long = tickPeriodMillis.toLong()

    private var duration: Duration = defaultDuration
    private var increment: Duration = defaultIncrement
    private var endMark: ComparableTimeMark = timeSource.markNow()
    private var tickingJob: Job? = null

    private val _whiteTime: MutableStateFlow<Duration> = MutableStateFlow(duration + increment)
    private val _blackTime: MutableStateFlow<Duration> = MutableStateFlow(duration + increment)
    private val _clockState: MutableStateFlow<ClockState> = MutableStateFlow(ClockState.FULL_RESET)
    private val _playerState: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.WHITE)

    val whiteTime: StateFlow<Duration> = _whiteTime.asStateFlow()
    val blackTime: StateFlow<Duration> = _blackTime.asStateFlow()
    val clockState: StateFlow<ClockState> = _clockState.asStateFlow()
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var currentTime: Duration
        get() = when (_playerState.value) {
            PlayerState.WHITE -> _whiteTime.value
            PlayerState.BLACK -> _blackTime.value
        }
        set(time) = when (_playerState.value) {
            PlayerState.WHITE -> _whiteTime.value = time
            PlayerState.BLACK -> _blackTime.value = time
        }

    private var savedTimeMinutes: Float = durationMinutes.toFloat()
        set(minutes) {
            val seconds = savedTimeSeconds.roundToInt()
            field = minutes.coerceIn(
                minimumValue = (-seconds / 60).toFloat() + if (seconds % 60 == 0) 1F else 0F,
                maximumValue = (599 - seconds / 60).toFloat(),
            )
        }

    private var savedTimeSeconds: Float = incrementSeconds.toFloat()
        set(seconds) {
            val minutes = savedTimeMinutes.roundToInt()
            field = seconds.coerceIn(
                minimumValue = (1 - minutes * 60).toFloat(),
                maximumValue = (35_999 - minutes * 60).toFloat(),
            )
        }

    private var savedDurationMinutes: Float = durationMinutes.toFloat()
        set(minutes) {
            val incrementSeconds = savedIncrementSeconds.roundToInt()
            field = minutes.coerceIn(
                minimumValue = if (incrementSeconds == 0) 1F else 0F,
                maximumValue = 180F,
            )
        }

    private var savedIncrementSeconds: Float = incrementSeconds.toFloat()
        set(seconds) {
            val durationMinutes = savedDurationMinutes.roundToInt()
            field = seconds.coerceIn(
                minimumValue = if (durationMinutes == 0) 1F else 0F,
                maximumValue = 30F,
            )
        }

    private suspend fun tickUntilFinished() {
        while (_clockState.value == ClockState.TICKING) {
            val remainingTime = endMark - timeSource.markNow()
            val delayTimeMillis = (remainingTime.inWholeMilliseconds - 1L) % tickPeriodMillis + 1L
            val delayTime = delayTimeMillis.milliseconds
            delay(delayTime)  // suspending function is cancellable here

            val newTime = remainingTime - delayTime
            if (newTime.isPositive()) {
                currentTime = newTime
            } else {
                currentTime = Duration.ZERO
                _clockState.value = ClockState.FINISHED
            }
        }
    }

    fun start() {
        tickingJob?.cancel()
        endMark = timeSource.markNow() + currentTime
        _clockState.value = ClockState.TICKING
        tickingJob = viewModelScope.launch { tickUntilFinished() }
    }

    fun play() {
        tickingJob?.cancel()
        val playMark = timeSource.markNow()
        val remainingTime = endMark - playMark
        currentTime = (remainingTime + increment).coerceAtMost(35_999.seconds)
        _playerState.update {
            when (it) {
                PlayerState.WHITE -> PlayerState.BLACK
                PlayerState.BLACK -> PlayerState.WHITE
            }
        }
        endMark = playMark + currentTime
        tickingJob = viewModelScope.launch { tickUntilFinished() }
    }

    fun pause() {
        tickingJob?.cancel()
        currentTime = endMark - timeSource.markNow()
        _clockState.value = ClockState.PAUSED
    }

    fun reset() {
        tickingJob?.cancel()
        if (_clockState.value == ClockState.SOFT_RESET) {
            duration = defaultDuration
            increment = defaultIncrement
        }
        _whiteTime.value = duration + increment
        _blackTime.value = duration + increment
        _playerState.value = PlayerState.WHITE
        _clockState.value = if (duration == defaultDuration && increment == defaultIncrement) {
            ClockState.FULL_RESET
        } else {
            ClockState.SOFT_RESET
        }
    }

    fun save() {
        when (_clockState.value) {
            ClockState.SOFT_RESET, ClockState.FULL_RESET -> run {
                savedDurationMinutes = duration.inWholeMinutes.toFloat()
                savedIncrementSeconds = increment.inWholeSeconds.toFloat()
            }

            else -> currentTime.toComponents { minutes, seconds, nanoseconds ->
                savedTimeMinutes = minutes.toFloat()
                savedTimeSeconds = seconds.toFloat() + nanoseconds.toFloat() / 1_000_000_000F
            }
        }
    }

    fun restore(
        addMinutes: Float = 0F, addSeconds: Float = 0F, isDecimalRestored: Boolean = false,
    ) {
        if (_clockState.value == ClockState.PAUSED) {
            savedTimeMinutes += addMinutes
            savedTimeSeconds += addSeconds
            val newMinutes = savedTimeMinutes.roundToInt().minutes
            val newSeconds = if (isDecimalRestored) {
                (savedTimeSeconds * 1_000F).roundToInt().milliseconds
            } else {
                savedTimeSeconds.roundToInt().seconds
            }
            val newTime = newMinutes + newSeconds
            val timeUpdate = newTime - currentTime
            val timeUpdateSign = if (timeUpdate.isPositive()) 1F else -1F
            val isValidMinutesUpdate = addMinutes.sign == timeUpdateSign
            val isValidSecondsUpdate = addSeconds.sign == timeUpdateSign
            val isNotAnUpdate = addMinutes == 0F && addSeconds == 0F
            if (isValidMinutesUpdate || isValidSecondsUpdate || isNotAnUpdate) {
                currentTime = newTime
                endMark = timeSource.markNow() + newTime
            }
        } else {
            tickingJob?.cancel()
            savedDurationMinutes += addMinutes
            savedIncrementSeconds += addSeconds
            duration = savedDurationMinutes.roundToInt().minutes
            increment = savedIncrementSeconds.roundToInt().seconds
            val newTime = duration + increment
            _whiteTime.value = newTime
            _blackTime.value = newTime
            _clockState.value = if (duration == defaultDuration && increment == defaultIncrement) {
                ClockState.FULL_RESET
            } else {
                ClockState.SOFT_RESET
            }
        }
    }
}
