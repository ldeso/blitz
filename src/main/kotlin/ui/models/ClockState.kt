// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

package net.leodesouza.blitz.ui.models

/**
 * Whether the clock is currently [TICKING], is [PAUSED], has [FINISHED] ticking, is in a state of
 * [SOFT_RESET] to its initial time, or is in a state of [FULL_RESET] to its initial configuration.
 */
enum class ClockState { TICKING, PAUSED, FINISHED, SOFT_RESET, FULL_RESET }
