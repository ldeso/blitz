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
 * ViewModel for the chess clock screen.
 *
 * @param[durationMinutes] Initial time for each player in minutes.
 * @param[incrementSeconds] Time increment in seconds.
 * @param[tickPeriod] Period between ticks in milliseconds.
 */
class ChessClockViewModel(
    durationMinutes: Long,
    incrementSeconds: Long,
    private val tickPeriod: Long,
) : ViewModel() {
    private val defaultDuration: Long = durationMinutes * 60_000L
    private val defaultIncrement: Long = incrementSeconds * 1_000L
    private var currentDuration: Long = defaultDuration
    private var currentIncrement: Long = defaultIncrement
    private var targetRealtime: Long = 0L

    private val _uiState: MutableStateFlow<ChessClockUiState> = MutableStateFlow(
        ChessClockUiState(
            whiteTime = defaultDuration + defaultIncrement,
            blackTime = defaultDuration + defaultIncrement,
        )
    )

    val uiState: StateFlow<ChessClockUiState> = _uiState.asStateFlow()

    private var savedDurationMinutes: Float = 0F
        set(minutes) {
            val incrementSeconds = savedIncrementSeconds.roundToLong()
            field = minutes.coerceIn(
                minimumValue = if (incrementSeconds == 0L) 1F else 0F,
                maximumValue = 180F,
            )
        }

    private var savedIncrementSeconds: Float = 0F
        set(seconds) {
            val durationMinutes = savedDurationMinutes.roundToLong()
            field = seconds.coerceIn(
                minimumValue = if (durationMinutes == 0L) 1F else 0F,
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

    fun start() {
        _uiState.update {
            targetRealtime = elapsedRealtime() + it.currentTime
            it.copy(isStarted = true, isTicking = true)
        }
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

    fun resetTime() {
        _uiState.update {
            val newTime = currentDuration + currentIncrement
            it.copy(whiteTime = newTime, blackTime = newTime, isWhiteTurn = true, isStarted = false)
        }
    }

    fun resetConf() {
        _uiState.update {
            currentDuration = defaultDuration
            currentIncrement = defaultIncrement
            val newTime = currentDuration + currentIncrement
            it.copy(whiteTime = newTime, blackTime = newTime, isDefaultConf = true)
        }
    }

    fun saveTime() {
        val currentTime = _uiState.value.currentTime
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
        _uiState.update {
            savedMinutes += addMinutes
            savedSeconds += addSeconds
            val newTime = savedMinutes.roundToLong() * 60_000L + if (isDecimalRestored) {
                (savedSeconds * 1_000F).roundToLong()
            } else {
                savedSeconds.roundToLong() * 1_000L
            }
            targetRealtime = elapsedRealtime() + newTime
            if (it.isWhiteTurn) {
                it.copy(whiteTime = newTime)
            } else {
                it.copy(blackTime = newTime)
            }
        }
    }

    fun restoreSavedConf(addMinutes: Float = 0F, addSeconds: Float = 0F) {
        _uiState.update {
            savedDurationMinutes += addMinutes
            savedIncrementSeconds += addSeconds
            currentDuration = savedDurationMinutes.roundToLong() * 60_000L
            currentIncrement = savedIncrementSeconds.roundToLong() * 1_000L
            val newTime = currentDuration + currentIncrement
            it.copy(whiteTime = newTime, blackTime = newTime, isDefaultConf = false)
        }
    }
}
