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

/**
 * UiState for the chess clock screen.
 *
 * @param[whiteTime] Remaining time for the first player in milliseconds.
 * @param[blackTime] Remaining time for the second player in milliseconds.
 * @param[isWhiteTurn] Whether it is the turn of the first or the second player.
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isTicking] Whether the clock is currently ticking.
 * @param[isDefaultConf] Whether the clock is set to its default configuration.
 */
data class ChessClockUiState(
    val whiteTime: Long,
    val blackTime: Long,
    val isWhiteTurn: Boolean = true,
    val isStarted: Boolean = false,
    val isTicking: Boolean = false,
    val isDefaultConf: Boolean = true,
) {
    val currentTime: Long
        get() = if (isWhiteTurn) whiteTime else blackTime

    val isFinished: Boolean
        get() = whiteTime <= 0L || blackTime <= 0L

    val isPaused: Boolean
        get() = !isTicking && !isFinished
}
