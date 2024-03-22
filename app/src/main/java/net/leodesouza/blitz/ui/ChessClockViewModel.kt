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

/**
 * UiState for the chess clock screen.
 *
 * @param[whiteTime] Remaining time for the first player in milliseconds.
 * @param[blackTime] Remaining time for the second player in milliseconds.
 * @param[isWhiteTurn] Whether it is the turn of the first or the second player.
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isTicking] Whether the clock is currently ticking.
 * @param[isDefaultConfig] Whether the clock is set to its default configuration.
 */
data class ChessClockUiState(
    val whiteTime: Long,
    val blackTime: Long,
    val isWhiteTurn: Boolean = true,
    val isStarted: Boolean = false,
    val isTicking: Boolean = false,
    val isDefaultConfig: Boolean = true,
) {
    val currentTime: Long
        get() = if (isWhiteTurn) whiteTime else blackTime

    val isFinished: Boolean
        get() = whiteTime <= 0L || blackTime <= 0L

    val isPaused: Boolean
        get() = !isTicking && !isFinished
}

/**
 * ViewModel for the chess clock screen.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriod] Period between each tick in milliseconds.
 * @param[onStart] Callback called when the clock starts ticking.
 * @param[onPause] Callback called when the clock stops ticking.
 */
class ChessClockViewModel(
    durationMinutes: Long,
    incrementSeconds: Long,
    private val tickPeriod: Long,
    private val onStart: () -> Unit = {},
    private val onPause: () -> Unit = {},
) : ViewModel() {
    private val defaultDuration = durationMinutes * 60_000L
    private val defaultIncrement = incrementSeconds * 1_000L
    private var currentDuration: Long = defaultDuration
    private var currentIncrement: Long = defaultIncrement
    private var targetRealtime: Long = 0L
    private val _uiState = MutableStateFlow(
        ChessClockUiState(
            whiteTime = defaultDuration + defaultIncrement,
            blackTime = defaultDuration + defaultIncrement,
        )
    )
    val uiState: StateFlow<ChessClockUiState> = _uiState.asStateFlow()

    fun start() {
        _uiState.update {
            targetRealtime = elapsedRealtime() + it.currentTime
            it.copy(isStarted = true, isTicking = true)
        }
        onStart.invoke()
    }

    fun pause() {
        _uiState.update {
            val newTime = targetRealtime - elapsedRealtime()
            if (it.isWhiteTurn) {
                it.copy(whiteTime = newTime, isTicking = false)
            } else {
                it.copy(blackTime = newTime, isTicking = false)
            }
        }
        onPause.invoke()
    }

    suspend fun tick() {
        _uiState.update {
            val remainingTime = targetRealtime - elapsedRealtime()
            val correctionDelay = remainingTime % tickPeriod
            delay(correctionDelay)
            val newTime = remainingTime - correctionDelay
            if (it.isWhiteTurn) {
                it.copy(whiteTime = newTime)
            } else {
                it.copy(blackTime = newTime)
            }
        }
    }

    fun nextPlayer() {
        _uiState.update {
            val remainingTime = targetRealtime - elapsedRealtime()
            val newTime = if (remainingTime > 0L) (remainingTime + currentIncrement) else 0L
            if (it.isWhiteTurn) {
                targetRealtime = elapsedRealtime() + it.blackTime
                it.copy(whiteTime = newTime, isWhiteTurn = false)
            } else {
                targetRealtime = elapsedRealtime() + it.whiteTime
                it.copy(blackTime = newTime, isWhiteTurn = true)
            }
        }
    }

    private fun applyConfig() {
        _uiState.update {
            val newTime = currentDuration + currentIncrement
            it.copy(whiteTime = newTime, blackTime = newTime)
        }
    }

    fun resetTime() {
        _uiState.update { it.copy(isWhiteTurn = true, isStarted = false) }
        applyConfig()
    }

    fun resetConfig() {
        currentDuration = defaultDuration
        currentIncrement = defaultIncrement
        _uiState.update { it.copy(isDefaultConfig = true) }
        applyConfig()
    }

    private var savedDurationMinutes: Float = 0F
        set(minutes) {
            field = minutes.coerceIn(
                minimumValue = if (currentIncrement < 1_000L) 1F else 0F,
                maximumValue = 180F,
            )
        }

    private var savedIncrementSeconds: Float = 0F
        set(seconds) {
            field = seconds.coerceIn(
                minimumValue = if (currentDuration < 60_000L) 1F else 0F,
                maximumValue = 30F,
            )
        }

    private var savedMinutes: Float = 0F
        set(minutes) {
            val seconds = savedSeconds.roundToLong()
            field = minutes.coerceIn(
                minimumValue = (-seconds / 60L).toFloat() + if (seconds % 60L == 0L) 1F else 0F,
                maximumValue = (599L - seconds / 60L).toFloat(),
            )
        }

    private var savedSeconds: Float = 0F
        set(seconds) {
            val minutes = savedMinutes.roundToLong()
            field = seconds.coerceIn(
                minimumValue = (1L - minutes * 60L).toFloat(),
                maximumValue = (35_999L - minutes * 60L).toFloat(),
            )
        }

    fun saveTime() {
        if (_uiState.value.isStarted) {
            val currentTime = _uiState.value.currentTime
            savedMinutes = (currentTime / 60_000L).toFloat()
            savedSeconds = (currentTime % 60_000L / 1_000L).toFloat()
        } else {
            savedDurationMinutes = (currentDuration / 60_000L).toFloat()
            savedIncrementSeconds = (currentIncrement / 1_000L).toFloat()
        }
    }

    fun addMinutesToSavedTime(minutes: Float) {
        _uiState.update {
            if (it.isStarted) {
                savedMinutes += minutes
                val newTime =
                    savedMinutes.roundToLong() * 60_000L + savedSeconds.roundToLong() * 1_000L
                if (it.isWhiteTurn) {
                    it.copy(whiteTime = newTime)
                } else {
                    it.copy(blackTime = newTime)
                }
            } else {
                savedDurationMinutes += minutes
                currentDuration = savedDurationMinutes.roundToLong() * 60_000L
                val newTime = currentDuration + currentIncrement
                it.copy(whiteTime = newTime, blackTime = newTime, isDefaultConfig = false)
            }
        }
    }

    fun addSecondsToSavedTime(seconds: Float) {
        _uiState.update {
            if (it.isStarted) {
                savedSeconds += seconds
                val newTime =
                    savedMinutes.roundToLong() * 60_000L + savedSeconds.roundToLong() * 1_000L
                if (it.isWhiteTurn) {
                    it.copy(whiteTime = newTime)
                } else {
                    it.copy(blackTime = newTime)
                }
            } else {
                savedIncrementSeconds += seconds
                currentIncrement = savedIncrementSeconds.roundToLong() * 1_000L
                val newTime = currentDuration + currentIncrement
                it.copy(whiteTime = newTime, blackTime = newTime, isDefaultConfig = false)
            }
        }
    }
}
