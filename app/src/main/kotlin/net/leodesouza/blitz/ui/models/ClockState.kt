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

package net.leodesouza.blitz.ui.models

/**
 * Whether the clock is currently [TICKING], is [PAUSED], has [FINISHED] ticking, is in a state of
 * [SOFT_RESET] to its initial time, or is in a state of [FULL_RESET] to its initial configuration.
 */
enum class ClockState { TICKING, PAUSED, FINISHED, SOFT_RESET, FULL_RESET }
