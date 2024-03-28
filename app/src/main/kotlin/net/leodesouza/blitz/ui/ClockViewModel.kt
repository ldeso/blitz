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
    private val defaultTime: Long = defaultDuration + defaultIncrement
    private var currentDuration: Long = defaultDuration
    private var currentIncrement: Long = defaultIncrement
    private var targetRealtime: Long = 0L

    private val _whiteTime: MutableStateFlow<Long> = MutableStateFlow(defaultTime)
    private val _blackTime: MutableStateFlow<Long> = MutableStateFlow(defaultTime)
    private val _uiState: MutableStateFlow<ClockUiState> = MutableStateFlow(ClockUiState())

    val whiteTime: StateFlow<Long> = _whiteTime.asStateFlow()
    val blackTime: StateFlow<Long> = _blackTime.asStateFlow()
    val uiState: StateFlow<ClockUiState> = _uiState.asStateFlow()

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

    fun start() {
        val currentTime = if (_uiState.value.isWhiteTurn) _whiteTime.value else _blackTime.value
        targetRealtime = elapsedRealtime() + currentTime
        _uiState.value = _uiState.value.copy(isStarted = true, isTicking = true)
    }

    fun pause() {
        val newTime = targetRealtime - elapsedRealtime()
        if (_uiState.value.isWhiteTurn) {
            _whiteTime.value = newTime
        } else {
            _blackTime.value = newTime
        }
        _uiState.value = _uiState.value.copy(isTicking = false, isFinished = newTime <= 0L)
    }

    suspend fun tick() {
        val remainingTime = targetRealtime - elapsedRealtime()
        val correctionDelay = remainingTime % tickPeriod
        delay(correctionDelay)
        val newTime = remainingTime - correctionDelay
        if (_uiState.value.isWhiteTurn) {
            _whiteTime.value = newTime
        } else {
            _blackTime.value = newTime
        }
        _uiState.value = _uiState.value.copy(isFinished = newTime <= 0L)
    }

    fun play() {
        val remainingTime = targetRealtime - elapsedRealtime()
        val newTime = if (remainingTime > 0L) (remainingTime + currentIncrement) else 0L
        if (_uiState.value.isWhiteTurn) {
            _whiteTime.value = newTime
            targetRealtime = elapsedRealtime() + _blackTime.value
        } else {
            _blackTime.value = newTime
            targetRealtime = elapsedRealtime() + _whiteTime.value
        }
        _uiState.update { it.copy(isWhiteTurn = !it.isWhiteTurn, isFinished = newTime <= 0L) }
    }

    fun resetTime() {
        val newTime = currentDuration + currentIncrement
        _whiteTime.value = newTime
        _blackTime.value = newTime
        _uiState.value = _uiState.value.copy(
            isWhiteTurn = true, isStarted = false, isFinished = false,
        )
    }

    fun resetConf() {
        currentDuration = defaultDuration
        currentIncrement = defaultIncrement
        val newTime = currentDuration + currentIncrement
        _whiteTime.value = newTime
        _blackTime.value = newTime
        _uiState.value = _uiState.value.copy(
            isWhiteTurn = true, isStarted = false, isFinished = false, isDefaultConf = true,
        )
    }

    fun saveTime() {
        val currentTime = if (_uiState.value.isWhiteTurn) _whiteTime.value else _blackTime.value
        savedMinutes = (currentTime / 60_000L).toFloat()
        savedSeconds = (currentTime % 60_000L).toFloat() / 1_000F
    }

    fun saveConf() {
        savedDurationMinutes = (currentDuration / 60_000L).toFloat()
        savedIncrementSeconds = (currentIncrement / 1_000L).toFloat()
    }

    fun restoreSavedTime(
        addMinutes: Float = 0F, addSeconds: Float = 0F, isDecimalRestored: Boolean = false,
    ) {
        savedMinutes += addMinutes
        savedSeconds += addSeconds
        val currentTime = if (_uiState.value.isWhiteTurn) _whiteTime.value else _blackTime.value
        val newTime = savedMinutes.roundToLong() * 60_000L + if (isDecimalRestored) {
            (savedSeconds * 1_000F).roundToLong()
        } else {
            savedSeconds.roundToLong() * 1_000L
        }
        val addTime = (newTime - currentTime).toFloat()
        val isGoodMinutesUpdate = addMinutes.sign == addTime.sign
        val isGoodSecondsUpdate = addSeconds.sign == addTime.sign
        val isNotAnUpdate = addMinutes == 0F && addSeconds == 0F
        if (isGoodMinutesUpdate || isGoodSecondsUpdate || isNotAnUpdate) {
            if (_uiState.value.isWhiteTurn) {
                _whiteTime.value = newTime
            } else {
                _blackTime.value = newTime
            }
            _uiState.value = _uiState.value.copy(isFinished = false)
            targetRealtime = elapsedRealtime() + newTime
        }
    }

    fun restoreSavedConf(addMinutes: Float = 0F, addSeconds: Float = 0F) {
        savedDurationMinutes += addMinutes
        savedIncrementSeconds += addSeconds
        currentDuration = savedDurationMinutes.roundToLong() * 60_000L
        currentIncrement = savedIncrementSeconds.roundToLong() * 1_000L
        val newTime = currentDuration + currentIncrement
        _whiteTime.value = newTime
        _blackTime.value = newTime
        _uiState.value = _uiState.value.copy(isDefaultConf = false)
    }
}
