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
 * UI state for the chess clock screen.
 *
 * @param[isWhiteTurn] Whether it is the turn of the first or the second player.
 * @param[isStarted] Whether the clock has started ticking.
 * @param[isTicking] Whether the clock is currently ticking.
 * @param[isFinished] Whether the clock has finished ticking.
 * @param[isDefaultConf] Whether the clock is set to its default configuration.
 */
data class ClockUiState(
    val isWhiteTurn: Boolean = true,
    val isStarted: Boolean = false,
    val isTicking: Boolean = false,
    val isFinished: Boolean = false,
    val isDefaultConf: Boolean = true,
) {
    val isPaused: Boolean
        get() = !isTicking && !isFinished
}
