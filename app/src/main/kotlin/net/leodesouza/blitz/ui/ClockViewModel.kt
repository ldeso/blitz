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

import android.os.SystemClock.elapsedRealtime
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.leodesouza.blitz.ui.models.ClockState
import net.leodesouza.blitz.ui.models.PlayerState
import kotlin.math.roundToLong
import kotlin.math.sign

/**
 * ViewModel holding state and logic for the chess clock screen.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriod] Period between ticks in milliseconds.
 */
class ClockViewModel(
    durationMinutes: Long,
    incrementSeconds: Long,
    private val tickPeriod: Long,
) : ViewModel() {
    private val defaultDuration: Long = durationMinutes * 60_000L
    private val defaultIncrement: Long = incrementSeconds * 1_000L
    private var duration: Long = defaultDuration
    private var increment: Long = defaultIncrement
    private var targetRealtime: Long = 0L

    private val _whiteTime: MutableStateFlow<Long> = MutableStateFlow(duration + increment)
    private val _blackTime: MutableStateFlow<Long> = MutableStateFlow(duration + increment)
    private val _clockState: MutableStateFlow<ClockState> = MutableStateFlow(ClockState.FULL_RESET)
    private val _playerState: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState.WHITE)

    val whiteTime: StateFlow<Long> = _whiteTime.asStateFlow()
    val blackTime: StateFlow<Long> = _blackTime.asStateFlow()
    val clockState: StateFlow<ClockState> = _clockState.asStateFlow()
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var savedMinutes: Float = durationMinutes.toFloat()
        set(minutes) {
            val seconds = savedSeconds.roundToLong()
            field = minutes.coerceIn(
                minimumValue = (-seconds / 60L).toFloat() + if (seconds % 60L == 0L) 1F else 0F,
                maximumValue = (599L - seconds / 60L).toFloat(),
            )
        }

    private var savedSeconds: Float = incrementSeconds.toFloat()
        set(seconds) {
            val minutes = savedMinutes.roundToLong()
            field = seconds.coerceIn(
                minimumValue = (1L - minutes * 60L).toFloat(),
                maximumValue = (35_999L - minutes * 60L).toFloat(),
            )
        }

    private var savedDurationMinutes: Float = durationMinutes.toFloat()
        set(minutes) {
            val incrementSeconds = savedIncrementSeconds.roundToLong()
            field = minutes.coerceIn(
                minimumValue = if (incrementSeconds == 0L) 1F else 0F,
                maximumValue = 180F,
            )
        }

    private var savedIncrementSeconds: Float = incrementSeconds.toFloat()
        set(seconds) {
            val durationMinutes = savedDurationMinutes.roundToLong()
            field = seconds.coerceIn(
                minimumValue = if (durationMinutes == 0L) 1F else 0F,
                maximumValue = 30F,
            )
        }

    fun start() {
        val currentTime = when (_playerState.value) {
            PlayerState.WHITE -> _whiteTime.value
            PlayerState.BLACK -> _blackTime.value
        }
        targetRealtime = elapsedRealtime() + currentTime
        _clockState.value = ClockState.TICKING
    }

    fun play() {
        val remainingTime = targetRealtime - elapsedRealtime()
        if (remainingTime > 0L) {
            val newTime = remainingTime + increment
            when (_playerState.value) {
                PlayerState.WHITE -> _whiteTime.value = newTime
                PlayerState.BLACK -> _blackTime.value = newTime
            }
            _playerState.update {
                when (it) {
                    PlayerState.WHITE -> PlayerState.BLACK
                    PlayerState.BLACK -> PlayerState.WHITE
                }
            }
            targetRealtime = when (_playerState.value) {
                PlayerState.WHITE -> elapsedRealtime() + _whiteTime.value
                PlayerState.BLACK -> elapsedRealtime() + _blackTime.value
            }
        } else {
            when (_playerState.value) {
                PlayerState.WHITE -> _whiteTime.value = 0L
                PlayerState.BLACK -> _blackTime.value = 0L
            }
            _clockState.value = ClockState.FINISHED
        }
    }

    fun pause() {
        val newTime = targetRealtime - elapsedRealtime()
        when (_playerState.value) {
            PlayerState.WHITE -> _whiteTime.value = newTime
            PlayerState.BLACK -> _blackTime.value = newTime
        }
        _clockState.value = ClockState.PAUSED
    }

    suspend fun tick() {
        val remainingTime = targetRealtime - elapsedRealtime()
        val correctionDelay = remainingTime % tickPeriod
        delay(correctionDelay)
        val newTime = remainingTime - correctionDelay
        when (_playerState.value) {
            PlayerState.WHITE -> _whiteTime.value = newTime
            PlayerState.BLACK -> _blackTime.value = newTime
        }
        if (newTime <= 0L) {
            _clockState.value = ClockState.FINISHED
        }
    }

    fun resetTime() {
        val newTime = duration + increment
        _whiteTime.value = newTime
        _blackTime.value = newTime
        _playerState.value = PlayerState.WHITE
        _clockState.value = ClockState.SOFT_RESET
    }

    fun resetConf() {
        duration = defaultDuration
        increment = defaultIncrement
        val newTime = duration + increment
        _whiteTime.value = newTime
        _blackTime.value = newTime
        _playerState.value = PlayerState.WHITE
        _clockState.value = ClockState.FULL_RESET
    }

    fun saveTime() {
        val currentTime = when (_playerState.value) {
            PlayerState.WHITE -> _whiteTime.value
            PlayerState.BLACK -> _blackTime.value
        }
        savedMinutes = (currentTime / 60_000L).toFloat()
        savedSeconds = (currentTime % 60_000L).toFloat() / 1_000F
    }

    fun saveConf() {
        savedDurationMinutes = (duration / 60_000L).toFloat()
        savedIncrementSeconds = (increment / 1_000L).toFloat()
    }

    fun restoreSavedTime(
        addMinutes: Float = 0F, addSeconds: Float = 0F, isDecimalRestored: Boolean = false,
    ) {
        savedMinutes += addMinutes
        savedSeconds += addSeconds
        val currentTime = when (_playerState.value) {
            PlayerState.WHITE -> _whiteTime.value
            PlayerState.BLACK -> _blackTime.value
        }
        val newTime = savedMinutes.roundToLong() * 60_000L + if (isDecimalRestored) {
            (savedSeconds * 1_000F).roundToLong()
        } else {
            savedSeconds.roundToLong() * 1_000L
        }
        val timeUpdate = (newTime - currentTime).toFloat()
        val isGoodMinutesUpdate = addMinutes.sign == timeUpdate.sign
        val isGoodSecondsUpdate = addSeconds.sign == timeUpdate.sign
        val isNotAnUpdate = addMinutes == 0F && addSeconds == 0F
        if (isGoodMinutesUpdate || isGoodSecondsUpdate || isNotAnUpdate) {
            when (_playerState.value) {
                PlayerState.WHITE -> _whiteTime.value = newTime
                PlayerState.BLACK -> _blackTime.value = newTime
            }
            targetRealtime = elapsedRealtime() + newTime
        }
    }

    fun restoreSavedConf(addMinutes: Float = 0F, addSeconds: Float = 0F) {
        savedDurationMinutes += addMinutes
        savedIncrementSeconds += addSeconds
        duration = savedDurationMinutes.roundToLong() * 60_000L
        increment = savedIncrementSeconds.roundToLong() * 1_000L
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
